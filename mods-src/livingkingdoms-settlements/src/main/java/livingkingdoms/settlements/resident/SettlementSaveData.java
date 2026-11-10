package livingkingdoms.settlements.resident;

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

/**
 * The resident records of a world, saved with the Overworld. The source of truth for our data:
 * it is readable while the entities are unloaded, which the simulation of M5 needs (ADR-0030).
 */
final class SettlementSaveData extends SavedData {
    private static final String NAME = "livingkingdoms_settlements";
    private static final String KEY_RESIDENTS = "residents";

    private final Map<StableId, ResidentRecord> residents = new LinkedHashMap<>();

    static SettlementSaveData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SettlementSaveData::new, SettlementSaveData::load), NAME);
    }

    private static SettlementSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        SettlementSaveData data = new SettlementSaveData();
        if (tag.contains(KEY_RESIDENTS)) {
            ResidentRecord.CODEC.listOf().parse(NbtOps.INSTANCE, tag.get(KEY_RESIDENTS))
                    .resultOrPartial(error -> LivingKingdomsSettlements.LOGGER.error("Resident records could not be read: {}", error))
                    .ifPresent(list -> list.forEach(record -> data.residents.put(record.id(), record)));
        }
        return data;
    }

    Optional<ResidentRecord> find(StableId id) {
        return Optional.ofNullable(residents.get(id));
    }

    Collection<ResidentRecord> all() {
        return Collections.unmodifiableCollection(residents.values());
    }

    void add(ResidentRecord record) {
        residents.put(record.id(), record);
        setDirty();
    }

    void remove(StableId id) {
        if (residents.remove(id) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(KEY_RESIDENTS, ResidentRecord.CODEC.listOf().encodeStart(NbtOps.INSTANCE, List.copyOf(residents.values())).getOrThrow());
        return tag;
    }
}
