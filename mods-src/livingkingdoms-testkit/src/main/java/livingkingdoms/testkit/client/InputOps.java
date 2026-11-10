package livingkingdoms.testkit.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Key path (convention A3): key mappings by name, held or clicked as if the player pressed them, and the
 * camera rotation. Movement, the inventory key and hotbar keys go through the game's own input handling.
 */
final class InputOps {
    private InputOps() {}

    static void register(OpRegistry registry) {
        registry.register("input.hold", InputOps::hold)
                .register("input.press", InputOps::press)
                .register("input.look", InputOps::look);
    }

    /** Holds a key mapping down for {@code ticks} client ticks, then releases it. */
    private static CompletableFuture<JsonElement> hold(JsonObject args) {
        String name = Args.required(args, "mapping", "\"mapping\": \"key.forward\"").getAsString();
        int ticks = Args.integer(args, "ticks", 1);
        return OnClient.call(() -> Args.mapping(name)).thenCompose(mapping -> TickTasks.onClientTicks(ticks + 1, tick -> {
            if (tick >= ticks) return (JsonElement) new JsonObject();
            mapping.setDown(true);
            return null;
        }, () -> new OpError("internal", "input.hold overran.", "Report a testkit bug."), () -> mapping.setDown(false)));
    }

    /** One click of a key mapping, handled by the game on the next client tick (only while no screen is open). */
    private static CompletableFuture<JsonElement> press(JsonObject args) {
        String name = Args.required(args, "mapping", "\"mapping\": \"key.inventory\"").getAsString();
        return OnClient.call(() -> {
            KeyMapping mapping = Args.mapping(name);
            KeyMapping.click(mapping.getKey());
            return mapping;
        }).thenCompose(mapping -> TickTasks.onClientTicks(2, tick -> tick >= 1 ? (JsonElement) new JsonObject() : null,
                () -> new OpError("internal", "input.press overran.", "Report a testkit bug."), () -> {}));
    }

    /** Sets the camera rotation in degrees: yaw 0 faces south (+Z), 90 west (−X), 180 north, 270 east (+X); pitch 90 looks down. */
    private static CompletableFuture<JsonElement> look(JsonObject args) {
        float yaw = (float) Args.number(args, "yaw", 0);
        float pitch = (float) Args.number(args, "pitch", 0);
        return OnClient.call(() -> {
            setRotation(player(), yaw, pitch);
            return new JsonObject();
        });
    }

    static void setRotation(LocalPlayer player, float yaw, float pitch) {
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
    }

    static LocalPlayer player() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) throw OnServer.noWorld();
        return player;
    }
}
