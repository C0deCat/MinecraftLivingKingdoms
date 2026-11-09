package livingkingdoms.testkit.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;

/** Runs work on the client thread: client state (screens, the local player, input) is only safe there. */
public final class OnClient {
    private OnClient() {}

    public static <T> CompletableFuture<T> call(Supplier<T> work) {
        return Minecraft.getInstance().submit(work);
    }
}
