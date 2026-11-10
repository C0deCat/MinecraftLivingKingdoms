package livingkingdoms.testkit.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

/** Truth from the integrated server for assertThat() (convention A1): blocks, the player's inventory, entities. */
final class QueryOps {
    private QueryOps() {}

    static void register(OpRegistry registry) {
        registry.register("query.block", QueryOps::block)
                .register("query.inventory", args -> OnServer.call(server -> inventory(OnServer.player(server))))
                .register("query.entities", QueryOps::entities);
    }

    private static CompletableFuture<JsonElement> block(JsonObject args) {
        BlockPos pos = Args.pos(args, "pos");
        return OnServer.call(server -> {
            ServerPlayer player = OnServer.player(server);
            BlockState state = player.serverLevel().getBlockState(pos);
            JsonObject result = new JsonObject();
            result.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            JsonObject properties = new JsonObject();
            for (Property<?> property : state.getProperties()) properties.addProperty(property.getName(), valueName(state, property));
            result.add("properties", properties);
            result.addProperty("hasBlockEntity", player.serverLevel().getBlockEntity(pos) != null);
            return result;
        });
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static JsonElement inventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        JsonArray slots = new JsonArray();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            JsonObject slot = Args.json(stack);
            slot.addProperty("slot", i);
            slots.add(slot);
        }
        JsonObject result = new JsonObject();
        result.add("slots", slots);
        result.addProperty("selectedSlot", inventory.selected);
        return result;
    }

    /** Entities of an optional type inside the box from–to (block coordinates, both corners included). */
    private static CompletableFuture<JsonElement> entities(JsonObject args) {
        BlockPos from = Args.pos(args, "from");
        BlockPos to = Args.pos(args, "to");
        String type = Args.string(args, "type", null);
        if (type != null && BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(type)).isEmpty()) {
            throw new OpError("bad_args", "Unknown entity type '" + type + "'.", "Use a registry id such as minecraft:zombie or minecraft:item.");
        }
        return OnServer.call(server -> {
            ServerPlayer player = OnServer.player(server);
            AABB box = AABB.encapsulatingFullBlocks(from, to);
            JsonArray list = new JsonArray();
            for (Entity entity : player.serverLevel().getEntities((Entity) null, box, e -> type == null || Args.typeId(e).equals(type))) {
                JsonObject json = new JsonObject();
                json.addProperty("id", entity.getId());
                json.addProperty("type", Args.typeId(entity));
                json.add("pos", Args.json(entity.position()));
                json.addProperty("alive", entity.isAlive());
                if (entity instanceof LivingEntity living) json.addProperty("health", living.getHealth());
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity item) json.add("item", Args.json(item.getItem()));
                list.add(json);
            }
            JsonObject result = new JsonObject();
            result.add("entities", list);
            return result;
        });
    }
}
