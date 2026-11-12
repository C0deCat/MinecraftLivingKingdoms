package livingkingdoms.settlements.survey;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import livingkingdoms.settlements.mca.McaAdapter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * {@code /lksettle survey [max]}: the M4-B measurement of the villages a world generates (the
 * structures in the tag {@code livingkingdoms_settlements:village_source}) inside the Overworld
 * border. Each village is generated and loaded, left alone for a while so its villagers appear
 * and MCA replaces them, and then measured: doors reachable on foot from the centre
 * ({@link Walkability}), who lives there, and open lava and fire next to its buildings.
 *
 * <p>Output: {@code <world>/livingkingdoms/villages.csv} and {@code village-doors.csv}, a log line
 * per village and a last line {@code Village survey done: ...}. One village at a time, on the
 * server thread; meant for the test server, where a long tick does no harm.
 */
public final class VillageSurvey {
    public static final TagKey<Structure> VILLAGE_SOURCE = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(LivingKingdomsSettlements.MOD_ID, "village_source"));

    /** Ticks a loaded village is left alone: MCA replaces one villager per tick, and POIs settle. */
    private static final int SETTLE_TICKS = 200;
    private static final int LOAD_TIMEOUT_TICKS = 20 * 600;
    /** Lava this close to a building sets it on fire sooner or later. */
    private static final int HAZARD_REACH = 4;

    private record Village(String id, StructureStart start) {}

    private record Row(String id, BlockPos centre, int pieces, Walkability.Result walk, int mca, int vanilla, int otherNpc, int golems,
            int unemployed, int withTrades, Map<String, Integer> professions, int openLava, int fire) {}

    private static VillageSurvey running;

    private final CommandSourceStack source;
    private final ServerLevel level;
    private final Deque<Village> queue = new ArrayDeque<>();
    private final List<Row> rows = new ArrayList<>();
    private final long startedMillis = System.currentTimeMillis();
    private final int found;
    private Village current;
    private List<ChunkPos> forced = List.of();
    private int waited;
    private int settled;
    private final LongSet hazardsSeen = new LongOpenHashSet();

    private VillageSurvey(CommandSourceStack source, ServerLevel level, List<Village> villages) {
        this.source = source;
        this.level = level;
        this.found = villages.size();
        this.queue.addAll(villages);
    }

    public static int start(CommandSourceStack source, int max) {
        if (running != null) {
            source.sendFailure(Component.literal("A village survey is already running"));
            return 0;
        }
        ServerLevel level = source.getServer().overworld();
        List<Village> villages = find(level);
        int total = villages.size();
        if (villages.size() > max) villages = villages.subList(0, max);
        running = new VillageSurvey(source, level, villages);
        String message = "Village survey: " + total + " village starts inside the border, measuring " + villages.size();
        LivingKingdomsSettlements.LOGGER.info(message);
        source.sendSuccess(() -> Component.literal(message), false);
        return villages.size();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (running != null && running.tick()) running = null;
    }

    /** Village starts inside the border, nearest to the world centre first. */
    private static List<Village> find(ServerLevel level) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        WorldBorder border = level.getWorldBorder();
        int minX = ((int) Math.floor(border.getMinX())) >> 4;
        int maxX = ((int) Math.ceil(border.getMaxX())) >> 4;
        int minZ = ((int) Math.floor(border.getMinZ())) >> 4;
        int maxZ = ((int) Math.ceil(border.getMaxZ())) >> 4;

        List<Village> villages = new ArrayList<>();
        for (Holder<Structure> holder : registry.getTagOrEmpty(VILLAGE_SOURCE)) {
            String id = holder.unwrapKey().map(key -> key.location().toString()).orElse("?");
            for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (!placement.isStructureChunk(state, x, z)) continue;
                        ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS);
                        StructureStart start = chunk.getStartForStructure(holder.value());
                        if (start == null || !start.isValid()) continue;
                        BlockPos centre = start.getBoundingBox().getCenter();
                        if (border.isWithinBounds(centre)) villages.add(new Village(id, start));
                    }
                }
            }
        }
        villages.sort(Comparator.comparingDouble(v -> v.start().getBoundingBox().getCenter().distSqr(BlockPos.ZERO)));
        return villages;
    }

    /** @return true when the survey is finished */
    private boolean tick() {
        if (current == null) {
            current = queue.poll();
            if (current == null) {
                finish();
                return true;
            }
            forced = chunksOf(current.start().getBoundingBox().inflatedBy(Walkability.MARGIN));
            for (ChunkPos pos : forced) level.setChunkForced(pos.x, pos.z, true);
            waited = 0;
            settled = 0;
            return false;
        }
        waited++;
        boolean loaded = forced.stream().allMatch(pos -> level.getChunkSource().getChunkNow(pos.x, pos.z) != null);
        if (!loaded && waited < LOAD_TIMEOUT_TICKS) return false;
        if (loaded && ++settled < SETTLE_TICKS) return false;

        if (loaded) {
            Row row = measure(current);
            rows.add(row);
            LivingKingdomsSettlements.LOGGER.info(describe(row));
        } else {
            LivingKingdomsSettlements.LOGGER.warn("Village survey: {} at {} did not load in {} ticks, skipped", current.id(),
                    current.start().getBoundingBox().getCenter().toShortString(), LOAD_TIMEOUT_TICKS);
        }
        for (ChunkPos pos : forced) level.setChunkForced(pos.x, pos.z, false);
        current = null;
        return false;
    }

    private Row measure(Village village) {
        BoundingBox box = village.start().getBoundingBox();
        Walkability.Result walk = Walkability.measure(level, box);

        int mca = 0;
        int vanilla = 0;
        int otherNpc = 0;
        int golems = 0;
        int unemployed = 0;
        int withTrades = 0;
        Map<String, Integer> professions = new TreeMap<>();
        AABB area = AABB.of(box).inflate(Walkability.MARGIN);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area, e -> e instanceof Npc || e instanceof IronGolem)) {
            if (entity instanceof IronGolem) {
                golems++;
                continue;
            }
            if (McaAdapter.body(entity).isPresent()) mca++;
            else if (entity.getClass() == Villager.class) vanilla++;
            else otherNpc++;
            if (entity instanceof Villager villager) {
                String profession = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString();
                professions.merge(profession, 1, Integer::sum);
                if (profession.equals("minecraft:none")) unemployed++;
                if (!villager.isBaby() && !villager.getOffers().isEmpty()) withTrades++;
            } else {
                professions.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), 1, Integer::sum);
            }
        }
        // Fire hazards: lava with air above it, and burning blocks, inside or right next to a piece of the village.
        int openLava = 0;
        int fire = 0;
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (StructurePiece piece : village.start().getPieces()) {
            BoundingBox near = piece.getBoundingBox().inflatedBy(HAZARD_REACH);
            for (BlockPos pos : BlockPos.betweenClosed(near.minX(), near.minY(), near.minZ(), near.maxX(), near.maxY(), near.maxZ())) {
                if (!hazardsSeen.add(pos.asLong())) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.FIRE)) fire++;
                else if (state.getFluidState().is(FluidTags.LAVA) && level.getBlockState(above.setWithOffset(pos, 0, 1, 0)).isAir()) openLava++;
            }
        }
        hazardsSeen.clear();
        return new Row(village.id(), box.getCenter(), village.start().getPieces().size(), walk, mca, vanilla, otherNpc, golems,
                unemployed, withTrades, professions, openLava, fire);
    }

    private static String describe(Row row) {
        Walkability.Result walk = row.walk();
        return String.format(Locale.ROOT, "Village %s at %s: %d pieces, doors %d/%d reachable from %d walkable positions, centre %s, residents %d MCA / %d vanilla / %d other, "
                        + "%d golems, %d with trades, open lava %d, fire %d, professions %s",
                row.id(), row.centre().toShortString(), row.pieces(), walk.reachedDoors(), walk.doors().size(), walk.reachedCells(),
                walk.centre().isEmpty() ? "none" : walk.bell() ? "bell" : "middle", row.mca(), row.vanilla(), row.otherNpc(), row.golems(),
                row.withTrades(), row.openLava(), row.fire(), row.professions());
    }

    private void finish() {
        Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("livingkingdoms");
        try {
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            lines.add("structure,x,y,z,pieces,centre,reached_cells,doors,doors_reached,share,mca,vanilla,other_npc,golems,unemployed,with_trades,open_lava,fire,professions");
            List<String> doorLines = new ArrayList<>();
            doorLines.add("structure,village_x,village_z,x,y,z,reached,gap");
            for (Row row : rows) {
                Walkability.Result walk = row.walk();
                int doors = walk.doors().size();
                lines.add(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%s,%d,%d,%d,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,\"%s\"",
                        row.id(), row.centre().getX(), row.centre().getY(), row.centre().getZ(), row.pieces(),
                        walk.centre().isEmpty() ? "none" : walk.bell() ? "bell" : "middle", walk.reachedCells(), doors, walk.reachedDoors(),
                        doors == 0 ? 0.0 : (double) walk.reachedDoors() / doors, row.mca(), row.vanilla(), row.otherNpc(), row.golems(),
                        row.unemployed(), row.withTrades(), row.openLava(), row.fire(), row.professions()));
                for (Walkability.Door door : walk.doors()) {
                    doorLines.add(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%b,%d", row.id(), row.centre().getX(), row.centre().getZ(),
                            door.pos().getX(), door.pos().getY(), door.pos().getZ(), door.reached(), door.gap()));
                }
            }
            Files.write(dir.resolve("villages.csv"), lines);
            Files.write(dir.resolve("village-doors.csv"), doorLines);
        } catch (IOException e) {
            LivingKingdomsSettlements.LOGGER.error("Could not write the village survey to {}", dir, e);
        }

        long doors = rows.stream().mapToLong(row -> row.walk().doors().size()).sum();
        long reached = rows.stream().mapToLong(row -> row.walk().reachedDoors()).sum();
        long fullyWalkable = rows.stream().filter(row -> !row.walk().doors().isEmpty() && row.walk().reachedDoors() == row.walk().doors().size()).count();
        int mca = rows.stream().mapToInt(Row::mca).sum();
        int vanilla = rows.stream().mapToInt(Row::vanilla).sum();
        int other = rows.stream().mapToInt(Row::otherNpc).sum();
        long withLava = rows.stream().filter(row -> row.openLava() > 0).count();
        long burning = rows.stream().filter(row -> row.fire() > 0).count();
        String done = String.format(Locale.ROOT, "Village survey done: %d of %d villages measured, doors %d/%d reachable (%.0f%%), "
                        + "%d villages fully walkable, %d with open lava next to a building, %d burning, residents %d MCA / %d vanilla / %d other, %d s -> %s",
                rows.size(), found, reached, doors, doors == 0 ? 0.0 : 100.0 * reached / doors, fullyWalkable, withLava, burning, mca, vanilla, other,
                (System.currentTimeMillis() - startedMillis) / 1000, dir.resolve("villages.csv").toAbsolutePath());
        LivingKingdomsSettlements.LOGGER.info(done);
        source.sendSuccess(() -> Component.literal(done), false);
    }

    private static List<ChunkPos> chunksOf(BoundingBox box) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
        }
        return chunks;
    }
}
