package livingkingdoms.testkit.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Reads op arguments and writes common results. Bad arguments become OpErrors with a hint. */
final class Args {
    private Args() {}

    static JsonElement required(JsonObject args, String key, String example) {
        JsonElement value = args.get(key);
        if (value == null || value.isJsonNull()) {
            throw new OpError("bad_args", "Missing argument '" + key + "'.", "Pass it, for example " + example + ".");
        }
        return value;
    }

    static String string(JsonObject args, String key, String fallback) {
        JsonElement value = args.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    static int integer(JsonObject args, String key, int fallback) {
        JsonElement value = args.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    static double number(JsonObject args, String key, double fallback) {
        JsonElement value = args.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsDouble();
    }

    static long longValue(JsonObject args, String key, long fallback) {
        JsonElement value = args.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsLong();
    }

    /** A block position given as {"x":..,"y":..,"z":..} (whole numbers). */
    static BlockPos pos(JsonObject args, String key) {
        JsonObject pos = required(args, key, "\"" + key + "\": {\"x\": 0, \"y\": -60, \"z\": 0}").getAsJsonObject();
        return new BlockPos(pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt());
    }

    static Direction face(JsonObject args, String key, Direction fallback) {
        String name = string(args, key, null);
        if (name == null) return fallback;
        Direction face = Direction.byName(name);
        if (face == null) throw new OpError("bad_args", "Unknown face '" + name + "'.", "Use one of: up, down, north, south, east, west.");
        return face;
    }

    /** A key mapping by its name, e.g. key.forward (convention A3: never by key code). Client thread. */
    static KeyMapping mapping(String name) {
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.getName().equals(name)) return mapping;
        }
        throw new OpError("unknown_mapping", "No key mapping named '" + name + "'.",
                "Use a key mapping name such as key.forward, key.jump, key.inventory, key.hotbar.1 (Options > Controls shows them).");
    }

    static JsonObject json(BlockPos pos) {
        JsonObject result = new JsonObject();
        result.addProperty("x", pos.getX());
        result.addProperty("y", pos.getY());
        result.addProperty("z", pos.getZ());
        return result;
    }

    static JsonObject json(Vec3 vec) {
        JsonObject result = new JsonObject();
        result.addProperty("x", vec.x);
        result.addProperty("y", vec.y);
        result.addProperty("z", vec.z);
        return result;
    }

    /** {item, count}; an empty stack is {item: "minecraft:air", count: 0}. */
    static JsonObject json(ItemStack stack) {
        JsonObject result = new JsonObject();
        result.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        result.addProperty("count", stack.getCount());
        return result;
    }

    static String typeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }
}
