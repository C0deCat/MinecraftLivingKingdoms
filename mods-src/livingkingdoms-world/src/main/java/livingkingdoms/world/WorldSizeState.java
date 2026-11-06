package livingkingdoms.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * The world size of the running save, known before any level or chunk is created. Worldgen code
 * (density functions run on worker threads) reads it here; one server runs per JVM, including the
 * integrated server in singleplayer.
 */
public final class WorldSizeState {
    private static volatile WorldSize current = WorldSize.DEFAULT;

    private WorldSizeState() {}

    public static WorldSize current() {
        return current;
    }

    static void onServerAboutToStart(ServerAboutToStartEvent event) {
        current = detect(event.getServer());
        LivingKingdomsWorld.LOGGER.info("World size of this save: {} ({} blocks)", current.id(), current.side());
    }

    static WorldSize detect(MinecraftServer server) {
        LevelStem nether = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.NETHER);
        if (nether == null) return WorldSize.DEFAULT;
        return nether.type().unwrapKey().flatMap(WorldSize::byNetherType).orElse(WorldSize.DEFAULT);
    }
}
