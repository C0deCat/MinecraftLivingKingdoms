package livingkingdoms.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * {@code livingkingdoms_world:fixed_count}: places a fixed number of starts per world size, for
 * content that must exist (strongholds, mega-dungeons, a boss), where a random spread leaves some
 * seeds without it. The inside of the dimension's border minus {@code edge_margin} is split into a
 * grid of about {@code count} cells; each chosen cell gets one point, moved to the nearest biome of
 * {@code preferred_biomes} within {@code search_radius} blocks, as vanilla does for stronghold rings.
 * Up to eight points per cell are tried: a point is taken when one of the set's structures generates a
 * valid start there (tested with the same call worldgen makes), so the count is met unless a whole cell
 * is unsuitable; the set then picks and generates its structure there as usual.
 *
 * <p>Positions depend on the seed, the salt and the world size of the save, and are computed once
 * per dimension, on first use. The biome source is not reachable from the structure state, so the
 * levels register themselves on load ({@link #onLevelLoad}).
 */
public final class FixedCountPlacement extends StructurePlacement {
    record Counts(int test, int small, int medium, int large) {
        static final Codec<Counts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 4096).optionalFieldOf("test", 0).forGetter(Counts::test),
                Codec.intRange(0, 4096).fieldOf("small").forGetter(Counts::small),
                Codec.intRange(0, 4096).fieldOf("medium").forGetter(Counts::medium),
                Codec.intRange(0, 4096).fieldOf("large").forGetter(Counts::large)
        ).apply(instance, Counts::new));

        int of(WorldSize size) {
            return switch (size) {
                case TEST -> test;
                case SMALL -> small;
                case MEDIUM -> medium;
                case LARGE -> large;
            };
        }
    }

    /** Our fields; grouped, because a codec group takes at most eight fields after the common ones. */
    record Settings(Counts counts, HolderSet<Biome> preferredBiomes, int biomeY, int edgeMargin, int searchRadius) {
        static final MapCodec<Settings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Counts.CODEC.fieldOf("counts").forGetter(Settings::counts),
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("preferred_biomes").forGetter(Settings::preferredBiomes),
                Codec.INT.optionalFieldOf("biome_y", 64).forGetter(Settings::biomeY),
                Codec.intRange(0, 8192).optionalFieldOf("edge_margin", 512).forGetter(Settings::edgeMargin),
                Codec.intRange(0, 1024).optionalFieldOf("search_radius", 160).forGetter(Settings::searchRadius)
        ).apply(instance, Settings::new));
    }

    static final MapCodec<FixedCountPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> placementCodec(instance)
            .and(Settings.CODEC.forGetter(p -> p.settings))
            .apply(instance, FixedCountPlacement::new));

    static final StructurePlacementType<FixedCountPlacement> TYPE = () -> CODEC;
    private static final int ATTEMPTS = 8;

    /** Levels by their structure state, registered on load: the placement needs their biome source. */
    private static final Map<ChunkGeneratorStructureState, ServerLevel> LEVELS = new ConcurrentHashMap<>();

    private final Settings settings;
    private final Map<ChunkGeneratorStructureState, Set<Long>> positions = new ConcurrentHashMap<>();

    private FixedCountPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                                Optional<ExclusionZone> exclusionZone, Settings settings) {
        super(locateOffset, method, frequency, salt, exclusionZone);
        this.settings = settings;
    }

    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) LEVELS.put(level.getChunkSource().getGeneratorState(), level);
    }

    static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) LEVELS.remove(level.getChunkSource().getGeneratorState());
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return positions.computeIfAbsent(state, this::computePositions).contains(ChunkPos.asLong(x, z));
    }

    /** The chunks of this world that get a start, for the validator and debugging. */
    public List<ChunkPos> positionsFor(ChunkGeneratorStructureState state) {
        return positions.computeIfAbsent(state, this::computePositions).stream().map(ChunkPos::new).toList();
    }

    private Set<Long> computePositions(ChunkGeneratorStructureState state) {
        ServerLevel level = LEVELS.get(state);
        if (level == null) {
            LivingKingdomsWorld.LOGGER.error("fixed_count placement used before its level was registered; no starts placed");
            return Set.of();
        }
        WorldSize size = WorldSizeState.current();
        int count = settings.counts().of(size);
        if (count == 0) return Set.of();
        // The border is not set yet while the spawn area generates, so the side comes from the size.
        int side = level.dimension() == Level.NETHER ? size.netherSide() : size.side();
        int inner = Math.max(16, side / 2 - settings.edgeMargin());
        int grid = (int) Math.ceil(Math.sqrt(count));
        double cell = 2.0 * inner / grid;

        RandomSource random = RandomSource.create(state.getLevelSeed() ^ (salt() * 0x9E3779B97F4A7C15L));
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < grid * grid; i++) cells.add(i);
        Collections.shuffle(cells, new java.util.Random(random.nextLong()));

        BiomeSource biomes = level.getChunkSource().getGenerator().getBiomeSource();
        List<Structure> structures = structuresOfThisSet(level);
        Set<Long> result = new HashSet<>();
        List<String> found = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = cells.get(i);
            ChunkPos chosen = null;
            String note = "";
            // Several points in the cell: the first whose biome fits and where one of the set's structures
            // actually generates wins, so the count is met unless the whole cell is unsuitable.
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                int x = (int) (-inner + (index % grid + 0.2 + 0.6 * random.nextDouble()) * cell);
                int z = (int) (-inner + (index / grid + 0.2 + 0.6 * random.nextDouble()) * cell);
                Pair<BlockPos, Holder<Biome>> hit = biomes.findBiomeHorizontal(x, settings.biomeY(), z,
                        settings.searchRadius(), settings.preferredBiomes()::contains, random, state.randomState().sampler());
                BlockPos pos = hit != null ? hit.getFirst() : new BlockPos(x, settings.biomeY(), z);
                ChunkPos chunk = new ChunkPos(pos);
                if (chosen == null) chosen = chunk;
                String generated = hit == null ? null : generatesAt(level, state, structures, chunk);
                if (generated != null) {
                    chosen = chunk;
                    note = generated + " after " + (attempt + 1);
                    break;
                }
                note = "none fits after " + (attempt + 1);
            }
            result.add(chosen.toLong());
            found.add(chosen.getMiddleBlockX() + "," + chosen.getMiddleBlockZ() + " " + note);
        }
        LivingKingdomsWorld.LOGGER.info("fixed_count salt {} in {} ({}): {} positions {}", salt(), level.dimension().location(), size.id(),
                result.size(), found);
        return result;
    }

    /** The structures of the structure set that uses this placement. */
    private List<Structure> structuresOfThisSet(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).stream()
                .filter(set -> set.placement() == this)
                .flatMap(set -> set.structures().stream())
                .map(entry -> entry.structure().value())
                .toList();
    }

    /** Id of the first structure of the set that generates a valid start in this chunk, or null. */
    private static String generatesAt(ServerLevel level, ChunkGeneratorStructureState state, List<Structure> structures, ChunkPos chunk) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        for (Structure structure : structures) {
            StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), state.randomState(),
                    level.getStructureManager(), state.getLevelSeed(), chunk, 0, level, structure.biomes()::contains);
            if (start.isValid()) {
                return level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure).getPath();
            }
        }
        return null;
    }

    @Override
    public StructurePlacementType<?> type() {
        return TYPE;
    }
}
