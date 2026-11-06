package livingkingdoms.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Applies the borders of the save's world size on every server start (ADR-0004).
 *
 * <p>Vanilla 1.21.1 copies the Overworld border with the same numbers to every other dimension, which
 * is right for the 1:1 End and Twilight Forest. The Nether gets side / ratio afterwards; it has to be
 * re-applied on each start because vanilla copies the Overworld border again when the levels load.
 */
final class WorldBorders {
    private WorldBorders() {}

    static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        WorldSize size = WorldSize.of(server).orElseGet(() -> {
            LivingKingdomsWorld.LOGGER.warn("Save has no Living Kingdoms size preset; assuming {}", WorldSize.DEFAULT.id());
            return WorldSize.DEFAULT;
        });

        set(server.overworld().getWorldBorder(), size.side());
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether != null) set(nether.getWorldBorder(), size.netherSide());

        LivingKingdomsWorld.LOGGER.info("World size {}: Overworld {}, Nether 1:{} -> {}", size.id(), size.side(), size.netherRatio(), size.netherSide());
        for (ServerLevel level : server.getAllLevels()) {
            LivingKingdomsWorld.LOGGER.info(
                "  {} border {} (coordinate_scale {})",
                level.dimension().location(), (int) level.getWorldBorder().getSize(), level.dimensionType().coordinateScale());
        }
    }

    private static void set(WorldBorder border, int side) {
        border.setCenter(0.0, 0.0);
        if (border.getSize() != side) border.setSize(side);
    }
}
