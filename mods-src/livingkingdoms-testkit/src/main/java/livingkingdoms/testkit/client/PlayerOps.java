package livingkingdoms.testkit.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Controller path (convention A3): the player acts through the client's game mode, as the game does after input,
 * so the server receives the same packets as from a real player. player.state reads the server player (truth).
 */
final class PlayerOps {
    private PlayerOps() {}

    static void register(OpRegistry registry) {
        registry.register("player.state", args -> OnServer.call(server -> state(OnServer.player(server))))
                .register("player.walkTo", PlayerOps::walkTo)
                .register("player.selectSlot", PlayerOps::selectSlot)
                .register("player.breakBlock", PlayerOps::breakBlock)
                .register("player.useItemOn", PlayerOps::useItemOn)
                .register("player.attack", PlayerOps::attack);
    }

    private static JsonElement state(ServerPlayer player) {
        JsonObject result = new JsonObject();
        result.add("pos", Args.json(player.position()));
        result.addProperty("yaw", player.getYRot());
        result.addProperty("pitch", player.getXRot());
        result.addProperty("onGround", player.onGround());
        result.addProperty("gamemode", player.gameMode.getGameModeForPlayer().getName());
        result.addProperty("health", player.getHealth());
        result.addProperty("food", player.getFoodData().getFoodLevel());
        result.addProperty("selectedSlot", player.getInventory().selected);
        result.addProperty("dimension", player.level().dimension().location().toString());
        return result;
    }

    /** Walks in a straight line (no pathfinding) by holding key.forward and turning toward the target each tick. */
    private static CompletableFuture<JsonElement> walkTo(JsonObject args) {
        JsonObject target = Args.required(args, "target", "\"target\": {\"x\": 10.5, \"y\": -60, \"z\": 0.5}").getAsJsonObject();
        double x = target.get("x").getAsDouble();
        double z = target.get("z").getAsDouble();
        double tolerance = Args.number(args, "tolerance", 0.5);
        int ticks = Args.integer(args, "ticks", 200);
        Minecraft mc = Minecraft.getInstance();
        return TickTasks.onClientTicks(ticks, tick -> {
            LocalPlayer player = InputOps.player();
            double dx = x - player.getX();
            double dz = z - player.getZ();
            if (Math.sqrt(dx * dx + dz * dz) <= tolerance) {
                mc.options.keyUp.setDown(false);
                return (JsonElement) Args.json(player.position());
            }
            InputOps.setRotation(player, (float) Math.toDegrees(Math.atan2(-dx, dz)), 0);
            mc.options.keyUp.setDown(true);
            return null;
        }, () -> new OpError("not_reached", "The player did not reach (" + x + ", " + z + ") within " + ticks + " ticks.",
                "Is the straight path blocked? walkTo does no pathfinding; clear the way in arrange() or give more ticks."),
                () -> mc.options.keyUp.setDown(false));
    }

    private static CompletableFuture<JsonElement> selectSlot(JsonObject args) {
        int slot = Args.integer(args, "slot", 0);
        if (slot < 0 || slot > 8) throw new OpError("bad_args", "Hotbar slot " + slot + " is out of range.", "Use 0–8.");
        return OnClient.call(() -> {
            InputOps.player().getInventory().selected = slot;
            return new JsonObject();
        });
    }

    /** Holds attack on the block until it breaks, as with a held mouse button. */
    private static CompletableFuture<JsonElement> breakBlock(JsonObject args) {
        BlockPos pos = Args.pos(args, "pos");
        int ticks = Args.integer(args, "ticks", 200);
        Minecraft mc = Minecraft.getInstance();
        Direction[] face = new Direction[1];
        return TickTasks.onClientTicks(ticks, tick -> {
            LocalPlayer player = InputOps.player();
            if (mc.level.getBlockState(pos).isAir()) return (JsonElement) new JsonObject();
            Vec3 center = Vec3.atCenterOf(pos);
            lookAt(player, center);
            if (tick == 0) {
                Vec3 eye = player.getEyePosition();
                face[0] = Direction.getNearest(eye.x - center.x, eye.y - center.y, eye.z - center.z);
                mc.gameMode.startDestroyBlock(pos, face[0]);
            } else {
                mc.gameMode.continueDestroyBlock(pos, face[0]);
            }
            player.swing(InteractionHand.MAIN_HAND);
            return null;
        }, () -> new OpError("not_broken", "The block at " + pos.toShortString() + " did not break within " + ticks + " ticks.",
                "Is it in reach (about 4.5 blocks), breakable in this gamemode, and is the right tool selected?"),
                () -> {
                    if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
                });
    }

    /** Right-clicks a face of a block with the selected item: places blocks, opens containers, uses items on blocks. */
    private static CompletableFuture<JsonElement> useItemOn(JsonObject args) {
        BlockPos pos = Args.pos(args, "pos");
        Direction face = Args.face(args, "face", Direction.UP);
        return OnClient.call(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = InputOps.player();
            Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            lookAt(player, hit);
            InteractionResult result = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
            if (result.shouldSwing()) player.swing(InteractionHand.MAIN_HAND);
            JsonObject json = new JsonObject();
            json.addProperty("result", result.name().toLowerCase());
            return json;
        });
    }

    /** Attacks an entity by its id (from query.entities), as a left click on it. */
    private static CompletableFuture<JsonElement> attack(JsonObject args) {
        int id = Args.required(args, "entity", "\"entity\": 123").getAsInt();
        return OnClient.call(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = InputOps.player();
            Entity entity = mc.level.getEntity(id);
            if (entity == null) {
                throw new OpError("not_found", "No entity with id " + id + " on the client.", "Take the id from query.entities, and check it is within render distance.");
            }
            lookAt(player, entity.getBoundingBox().getCenter());
            mc.gameMode.attack(player, entity);
            player.swing(InteractionHand.MAIN_HAND);
            return new JsonObject();
        });
    }

    static void lookAt(LocalPlayer player, Vec3 target) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        InputOps.setRotation(player, player.getYRot(), player.getXRot());
    }
}
