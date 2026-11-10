package livingkingdoms.testkit.client;

import com.google.gson.JsonObject;

/** Test support: waits counted in integrated server ticks (convention W2). */
final class TestOps {
    /** Five minutes of game time; a longer wait is a hung test, not a slow one. */
    private static final int MAX_TICKS = 20 * 60 * 5;

    private TestOps() {}

    static void register(OpRegistry registry) {
        registry.register("wait.ticks", args -> {
            int ticks = Args.integer(args, "n", 1);
            if (ticks < 1 || ticks > MAX_TICKS) throw new OpError("bad_args", "wait.ticks n=" + ticks + " is out of range.", "Use 1–" + MAX_TICKS + ".");
            return TickTasks.serverTicks(ticks).thenApply(now -> {
                JsonObject result = new JsonObject();
                result.addProperty("serverTicks", now);
                return result;
            });
        });
    }
}
