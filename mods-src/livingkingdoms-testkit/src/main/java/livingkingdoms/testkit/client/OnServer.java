package livingkingdoms.testkit.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Runs work on the integrated server thread: world state (blocks, entities, inventories) is only safe there. */
public final class OnServer {
    private OnServer() {}

    public static <T> CompletableFuture<T> call(Function<MinecraftServer, T> work) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return CompletableFuture.failedFuture(noWorld());
        return server.submit(() -> work.apply(server));
    }

    /** The singleplayer player on the server side. */
    public static ServerPlayer player(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            throw new OpError("no_player", "The world has no player yet.", "Wait until the world is open (world.create / world.open finish when the player is in).");
        }
        return server.getPlayerList().getPlayers().get(0);
    }

    static OpError noWorld() {
        return new OpError("no_world", "No world is open.", "Open a world first: specs get one from the world fixture; ops: world.create or world.open.");
    }
}
