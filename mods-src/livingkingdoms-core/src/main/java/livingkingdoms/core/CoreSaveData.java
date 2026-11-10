package livingkingdoms.core;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide state of Core, saved with the Overworld: the save format version of our mods and the
 * counters stable IDs come from. Server thread only.
 */
public final class CoreSaveData extends SavedData {
    /** Raised when a change makes older saves need a migration. */
    public static final int FORMAT_VERSION = 1;

    private static final String NAME = "livingkingdoms_core";
    private static final String KEY_FORMAT = "format_version";
    private static final String KEY_COUNTERS = "id_counters";

    private final int formatVersion;
    private final Map<String, Long> counters = new HashMap<>();

    private CoreSaveData(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public static CoreSaveData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new CoreSaveData(FORMAT_VERSION), CoreSaveData::load), NAME);
    }

    private static CoreSaveData load(CompoundTag tag, HolderLookup.Provider registries) {
        CoreSaveData data = new CoreSaveData(tag.getInt(KEY_FORMAT));
        if (data.formatVersion > FORMAT_VERSION) {
            LivingKingdomsCore.LOGGER.error("This world was saved with save format {} of Living Kingdoms, newer than {} of the installed mods",
                    data.formatVersion, FORMAT_VERSION);
        }
        CompoundTag saved = tag.getCompound(KEY_COUNTERS);
        for (String kind : saved.getAllKeys()) data.counters.put(kind, saved.getLong(kind));
        return data;
    }

    /** The save format this world was last written with. */
    public int formatVersion() {
        return formatVersion;
    }

    /** A new ID of the given kind; numbers of one kind are never given out twice in a world. */
    public StableId next(String kind) {
        long number = counters.merge(kind, 1L, Long::sum);
        setDirty();
        return new StableId(kind, number);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(KEY_FORMAT, FORMAT_VERSION);
        CompoundTag saved = new CompoundTag();
        counters.forEach(saved::putLong);
        tag.put(KEY_COUNTERS, saved);
        return tag;
    }
}
