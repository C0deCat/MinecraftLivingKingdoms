package livingkingdoms.settlements.settlement;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import livingkingdoms.core.CoreSaveData;
import livingkingdoms.core.StableId;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import livingkingdoms.settlements.mca.McaAdapter;
import livingkingdoms.settlements.resident.Residents;
import livingkingdoms.settlements.survey.VillageSurvey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The settlements of a world. In slice S1 every village the world generates (structure tag
 * {@code village_source}) becomes a settlement the first time one of its chunks loads (D-53,
 * D-54), each loaded chunk of it is made safe from lava once (TD-51), and the MCA villagers
 * inside it become its residents.
 *
 * <p>Server thread only. Chunk loads are only queued; the work happens on the next server tick,
 * because loading another chunk from inside a chunk-load event can deadlock.
 */
public final class Settlements {
    public static final String ID_KIND = "settlement";
    private static final int CHUNKS_PER_TICK = 8;

    /** A loaded chunk that a village structure reaches into. */
    private record Pending(Structure structure, long startChunk, ChunkPos chunk) {}

    private static final Deque<Pending> PENDING = new ArrayDeque<>();
    private static Set<Structure> sources;

    private Settlements() {}

    public static Collection<SettlementRecord> all(MinecraftServer server) {
        return SettlementStore.get(server).all();
    }

    public static Optional<SettlementRecord> find(MinecraftServer server, StableId id) {
        return SettlementStore.get(server).find(id);
    }

    /** The settlement whose bounds hold the position, if any. */
    public static Optional<SettlementRecord> at(MinecraftServer server, BlockPos pos) {
        for (SettlementRecord record : SettlementStore.get(server).all()) {
            if (record.contains(pos)) return Optional.of(record);
        }
        return Optional.empty();
    }

    /** Creates a settlement, or returns the one that already exists for the start chunk. */
    public static SettlementRecord found(MinecraftServer server, String source, long startChunk, BoundingBox bounds) {
        SettlementStore store = SettlementStore.get(server);
        SettlementRecord existing = store.byStart(startChunk);
        if (existing != null) return existing;
        SettlementRecord record = new SettlementRecord(CoreSaveData.get(server).next(ID_KIND), source, startChunk, bounds, List.of());
        store.add(record);
        LivingKingdomsSettlements.LOGGER.debug("Settlement {} founded from {} at {}", record.id(), source, bounds.getCenter().toShortString());
        return record;
    }

    public static boolean remove(MinecraftServer server, StableId id) {
        return SettlementStore.get(server).remove(id);
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        ChunkAccess chunk = event.getChunk();
        Map<Structure, LongSet> references = chunk.getAllReferences();
        if (references.isEmpty()) return;
        Set<Structure> villageSources = sources(level);
        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
            if (!villageSources.contains(entry.getKey())) continue;
            for (long start : entry.getValue()) PENDING.add(new Pending(entry.getKey(), start, chunk.getPos()));
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        for (int i = 0; i < CHUNKS_PER_TICK && !PENDING.isEmpty(); i++) adopt(level, PENDING.poll());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
        sources = null;
    }

    /** An MCA villager that appears inside a settlement becomes its resident. */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        McaAdapter.body(event.getEntity()).ifPresent(body -> {
            if (Residents.settlementOf(body).isPresent()) return;
            at(level.getServer(), body.blockPosition()).ifPresent(settlement -> Residents.join(body, settlement.id()));
        });
    }

    private static void adopt(ServerLevel level, Pending pending) {
        SettlementStore store = SettlementStore.get(level.getServer());
        SettlementRecord record = store.byStart(pending.startChunk());
        long chunkKey = pending.chunk().toLong();
        if (record != null && record.isSafe(chunkKey)) return;

        ChunkPos startPos = new ChunkPos(pending.startChunk());
        StructureStart start = level.getChunk(startPos.x, startPos.z, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(pending.structure());
        if (start == null || !start.isValid()) return;
        if (record == null) {
            String source = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(pending.structure()).toString();
            record = found(level.getServer(), source, pending.startChunk(), start.getBoundingBox());
        }
        if (level.getChunkSource().getChunkNow(pending.chunk().x, pending.chunk().z) == null) return;

        List<BoundingBox> buildings = start.getPieces().stream().map(StructurePiece::getBoundingBox).toList();
        BoundingBox column = new BoundingBox(pending.chunk().getMinBlockX(), level.getMinBuildHeight(), pending.chunk().getMinBlockZ(),
                pending.chunk().getMaxBlockX(), level.getMaxBuildHeight() - 1, pending.chunk().getMaxBlockZ());
        int changed = SiteSafety.clear(level, buildings, column);
        if (changed > 0) {
            LivingKingdomsSettlements.LOGGER.debug("Settlement {}: {} lava or fire blocks removed in chunk {}", record.id(), changed, pending.chunk());
        }
        record.markSafe(chunkKey);
        store.setDirty();
    }

    private static Set<Structure> sources(ServerLevel level) {
        if (sources == null) {
            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Set<Structure> found = new HashSet<>();
            for (Holder<Structure> holder : registry.getTagOrEmpty(VillageSurvey.VILLAGE_SOURCE)) found.add(holder.value());
            sources = found;
        }
        return sources;
    }
}
