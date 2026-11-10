package livingkingdoms.testkit.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * The open menu as the client sees it, and clicks on its slots through the client's game mode (the packets a
 * real click sends). Slot indexes are menu indexes, e.g. the player inventory: 0 craft result, 1–4 craft grid,
 * 36–44 hotbar.
 */
final class ContainerOps {
    private ContainerOps() {}

    static void register(OpRegistry registry) {
        registry.register("container.read", args -> OnClient.call(ContainerOps::read))
                .register("container.click", ContainerOps::click)
                .register("container.close", args -> OnClient.call(ContainerOps::close));
    }

    private static JsonElement read() {
        Minecraft mc = Minecraft.getInstance();
        AbstractContainerMenu menu = InputOps.player().containerMenu;
        JsonObject result = new JsonObject();
        result.addProperty("containerId", menu.containerId);
        result.addProperty("type", typeOf(menu));
        result.addProperty("screen", mc.screen == null ? null : mc.screen.getClass().getName());
        JsonArray slots = new JsonArray();
        for (int i = 0; i < menu.slots.size(); i++) {
            JsonObject slot = Args.json(menu.slots.get(i).getItem());
            slot.addProperty("index", i);
            slots.add(slot);
        }
        result.add("slots", slots);
        result.add("carried", Args.json(menu.getCarried()));
        return result;
    }

    /** Menu type id; the player's own inventory menu has no registered type and reports minecraft:inventory. */
    static String typeOf(AbstractContainerMenu menu) {
        if (menu instanceof InventoryMenu) return "minecraft:inventory";
        return String.valueOf(BuiltInRegistries.MENU.getKey(menu.getType()));
    }

    private static CompletableFuture<JsonElement> click(JsonObject args) {
        int slot = Args.required(args, "slot", "\"slot\": 36").getAsInt();
        int button = Args.integer(args, "button", 0);
        String typeName = Args.string(args, "type", "pickup");
        ClickType type;
        try {
            type = ClickType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new OpError("bad_args", "Unknown click type '" + typeName + "'.", "Use pickup, quick_move, swap, clone, throw, quick_craft or pickup_all.");
        }
        return OnClient.call(() -> {
            LocalPlayer player = InputOps.player();
            AbstractContainerMenu menu = player.containerMenu;
            if (slot < 0 || slot >= menu.slots.size()) {
                throw new OpError("bad_args", "Slot " + slot + " is outside the open menu (" + typeOf(menu) + ", " + menu.slots.size() + " slots).",
                        "Read the menu with container.read; is the expected container open?");
            }
            Minecraft.getInstance().gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, player);
            return new JsonObject();
        });
    }

    /** Closes the open container screen (or any screen) and tells the server, like pressing Escape. */
    private static JsonElement close() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && player.containerMenu != player.inventoryMenu) player.closeContainer();
        else if (mc.screen != null) mc.setScreen(null);
        return new JsonObject();
    }
}
