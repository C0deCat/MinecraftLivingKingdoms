package livingkingdoms.world;

import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Sends each player the border of the dimension they are in.
 *
 * <p>Vanilla 1.21.1 {@code PlayerList.sendLevelInfo} always sends the Overworld border, so in our
 * smaller Nether the client would show and collide with the Overworld border while the server
 * damages players past the real one. These events fire after {@code sendLevelInfo}, so our packet wins.
 */
final class BorderSync {
    private BorderSync() {}

    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        send(event.getEntity());
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        send(event.getEntity());
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        send(event.getEntity());
    }

    private static void send(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundInitializeBorderPacket(serverPlayer.serverLevel().getWorldBorder()));
        }
    }
}
