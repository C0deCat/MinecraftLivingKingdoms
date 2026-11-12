package livingkingdoms.settlements.settlement;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import livingkingdoms.core.StableId;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** The settlement records of a world, saved with the Overworld (ADR-0030: readable while nothing is loaded). */
final class SettlementStore extends SavedData {
    private static final String NAME = "livingkingdoms_settlement_sites";
    private static final String KEY = "settlements";

    private final Map<StableId, SettlementRecord> byId = new LinkedHashMap<>();
    private final Long2ObjectMap<SettlementRecord> byStart = new Long2ObjectOpenHashMap<>();

    static SettlementStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SettlementStore::new, SettlementStore::load), NAME);
    }

    private static SettlementStore load(CompoundTag tag, HolderLookup.Provider registries) {
        SettlementStore store = new SettlementStore();
        if (tag.contains(KEY)) {
            SettlementRecord.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get(KEY))
                    .resultOrPartial(error -> LivingKingdomsSettlements.LOGGER.error("Settlement records could not be read: {}", error))
                    .ifPresent(list -> list.forEach(store::put));
        }
        return store;
    }

    private void put(SettlementRecord record) {
        byId.put(record.id(), record);
        byStart.put(record.startChunk(), record);
    }

    Optional<SettlementRecord> find(StableId id) {
        return Optional.ofNullable(byId.get(id));
    }

    SettlementRecord byStart(long startChunk) {
        return byStart.get(startChunk);
    }

    Collection<SettlementRecord> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    void add(SettlementRecord record) {
        put(record);
        setDirty();
    }

    boolean remove(StableId id) {
        SettlementRecord record = byId.remove(id);
        if (record == null) return false;
        byStart.remove(record.startChunk());
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(KEY, SettlementRecord.CODEC.listOf().encodeStart(NbtOps.INSTANCE, List.copyOf(byId.values())).getOrThrow());
        return tag;
    }
}
