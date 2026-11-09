package livingkingdoms.testkit.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;

/** One protocol op. Runs its work on the thread that owns the state it touches and completes the future. */
@FunctionalInterface
public interface Op {
    CompletableFuture<JsonElement> run(JsonObject args);
}
