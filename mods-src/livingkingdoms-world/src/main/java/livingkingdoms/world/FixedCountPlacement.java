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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
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
    record Settings(Counts counts, HolderSet<Biome> preferredBiomes, int biomeY, int edgeMargin, int searchRadius,
                    List<ResourceLocation> floatingOk) {
        static final MapCodec<Settings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Counts.CODEC.fieldOf("counts").forGetter(Settings::counts),
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("preferred_biomes").forGetter(Settings::preferredBiomes),
                Codec.INT.optionalFieldOf("biome_y", 64).forGetter(Settings::biomeY),
                Codec.intRange(0, 8192).optionalFieldOf("edge_margin", 512).forGetter(Settings::edgeMargin),
                Codec.intRange(0, 1024).optionalFieldOf("search_radius", 160).forGetter(Settings::searchRadius),
                ResourceLocation.CODEC.listOf().optionalFieldOf("floating_ok", List.of()).forGetter(Settings::floatingOk)
        ).apply(instance, Settings::new));
    }

    static final MapCodec<FixedCountPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> placementCodec(instance)
            .and(Settings.CODEC.forGetter(p -> p.settings))
            .apply(instance, FixedCountPlacement::new));

    static final StructurePlacementType<FixedCountPlacement> TYPE = () -> CODEC;
    private static final int ATTEMPTS = 8;
    private static final int LATTICE = 128;
    private static final int LATTICE_TESTS = 48;

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

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BiomeSource biomes = generator.getBiomeSource();
        List<StructureSet.StructureSelectionEntry> structures = entriesOfThisSet(level);
        Set<Long> result = new HashSet<>();
        List<String> found = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = cells.get(i);
            ChunkPos chosen = null;
            String note = "";
            // Random points in the cell, each moved to the nearest preferred biome; then, for rare biomes, a
            // lattice over the whole cell. The first point where one of the set's structures generates and
            // stands on the ground wins, so the count is met unless the whole cell is unsuitable.
            for (int attempt = 0; attempt < ATTEMPTS && note.isEmpty(); attempt++) {
                int x = (int) (-inner + (index % grid + 0.2 + 0.6 * random.nextDouble()) * cell);
                int z = (int) (-inner + (index / grid + 0.2 + 0.6 * random.nextDouble()) * cell);
                Pair<BlockPos, Holder<Biome>> hit = biomes.findBiomeHorizontal(x, settings.biomeY(), z,
                        settings.searchRadius(), settings.preferredBiomes()::contains, random, state.randomState().sampler());
                ChunkPos chunk = new ChunkPos(hit != null ? hit.getFirst() : new BlockPos(x, settings.biomeY(), z));
                if (chosen == null) chosen = chunk;
                String generated = hit == null ? null : generatesAt(level, state, structures, chunk);
                if (generated != null) {
                    chosen = chunk;
                    note = generated + " after " + (attempt + 1);
                }
            }
            if (note.isEmpty()) {
                List<int[]> lattice = new ArrayList<>();
                double x0 = -inner + (index % grid) * cell;
                double z0 = -inner + (index / grid) * cell;
                for (double dx = LATTICE / 2.0; dx < cell; dx += LATTICE) {
                    for (double dz = LATTICE / 2.0; dz < cell; dz += LATTICE) lattice.add(new int[] {(int) (x0 + dx), (int) (z0 + dz)});
                }
                Collections.shuffle(lattice, new java.util.Random(random.nextLong()));
                int tests = 0;
                for (int[] p : lattice) {
                    if (tests >= LATTICE_TESTS) break;
                    // At the surface: biomes are 3D, and under a mountain y 64 is already an underground biome.
                    int surface = generator.getBaseHeight(p[0], p[1], Heightmap.Types.WORLD_SURFACE_WG, level, state.randomState());
                    Holder<Biome> biome = biomes.getNoiseBiome(p[0] >> 2, surface >> 2, p[1] >> 2, state.randomState().sampler());
                    if (!settings.preferredBiomes().contains(biome)) continue;
                    tests++;
                    ChunkPos chunk = new ChunkPos(new BlockPos(p[0], surface, p[1]));
                    String generated = generatesAt(level, state, structures, chunk);
                    if (generated != null) {
                        chosen = chunk;
                        note = generated + " on the lattice after " + tests;
                        break;
                    }
                }
                if (note.isEmpty()) note = "none fits (" + ATTEMPTS + " points, " + tests + " lattice tests)";
            }
            result.add(chosen.toLong());
            found.add(chosen.getMiddleBlockX() + "," + chosen.getMiddleBlockZ() + " " + note);
        }
        LivingKingdomsWorld.LOGGER.info("fixed_count salt {} in {} ({}): {} positions {}", salt(), level.dimension().location(), size.id(),
                result.size(), found);
        return result;
    }

    /** The entries of the structure set that uses this placement. */
    private List<StructureSet.StructureSelectionEntry> entriesOfThisSet(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).stream()
                .filter(set -> set.placement() == this)
                .flatMap(set -> set.structures().stream())
                .toList();
    }

    /**
     * Id of the structure that worldgen will place in this chunk, if it stands on the ground (entrance
     * not floating, no piece mostly hanging, see {@link StructureScan#grounded}; skipped for
     * {@code floating_ok}), or null. The pick repeats vanilla {@code ChunkGenerator.createStructures}:
     * entries drawn by weight with the chunk's large-feature seed, the first valid start wins.
     */
    private String generatesAt(ServerLevel level, ChunkGeneratorStructureState state, List<StructureSet.StructureSelectionEntry> entries,
                               ChunkPos chunk) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        List<StructureSet.StructureSelectionEntry> left = new ArrayList<>(entries);
        WorldgenRandom pick = new WorldgenRandom(new LegacyRandomSource(0L));
        pick.setLargeFeatureSeed(state.getLevelSeed(), chunk.x, chunk.z);
        int total = 0;
        for (StructureSet.StructureSelectionEntry entry : left) total += entry.weight();
        while (!left.isEmpty()) {
            int index = 0;
            if (entries.size() > 1) {
                int roll = pick.nextInt(total);
                for (StructureSet.StructureSelectionEntry entry : left) {
                    roll -= entry.weight();
                    if (roll < 0) break;
                    index++;
                }
            }
            StructureSet.StructureSelectionEntry entry = left.get(index);
            Structure structure = entry.structure().value();
            StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), state.randomState(),
                    level.getStructureManager(), state.getLevelSeed(), chunk, 0, level, structure.biomes()::contains);
            if (start.isValid()) {
                ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
                return settings.floatingOk().contains(id) || StructureScan.grounded(level, structure, start) ? id.getPath() : null;
            }
            left.remove(index);
            total -= entry.weight();
        }
        return null;
    }

    @Override
    public StructurePlacementType<?> type() {
        return TYPE;
    }
}
