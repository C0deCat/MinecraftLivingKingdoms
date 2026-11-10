package livingkingdoms.testkit.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import livingkingdoms.testkit.LivingKingdomsTestkit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;

/**
 * World ops: create a flat test world, open, leave, delete and list saves. Test worlds use the testkit's flat
 * preset (D-23) and are named with the {@code e2e-} prefix, so deletion never touches a player's world.
 */
final class WorldOps {
    static final String PREFIX = "e2e-";
    private static final ResourceKey<WorldPreset> FLAT = ResourceKey.create(Registries.WORLD_PRESET,
            ResourceLocation.fromNamespaceAndPath(LivingKingdomsTestkit.MOD_ID, "flat"));
    /** Client ticks to wait for a world to load and the player to join (about two minutes). */
    private static final int JOIN_TICKS = 20 * 120;
    private static final long DEFAULT_SEED = 20261110L;
    /** A dialog shown this long (two seconds) while opening a world is waiting for input, not loading. */
    private static final int DIALOG_TICKS = 40;

    private WorldOps() {}

    static void register(OpRegistry registry) {
        registry.register("world.create", WorldOps::create)
                .register("world.open", WorldOps::open)
                .register("world.leave", args -> OnClient.call(WorldOps::leave))
                .register("world.delete", WorldOps::delete)
                .register("world.list", args -> OnClient.call(WorldOps::list));
    }

    private static CompletableFuture<JsonElement> create(JsonObject args) {
        String name = testWorldName(args);
        long seed = Args.longValue(args, "seed", DEFAULT_SEED);
        GameType gameMode = GameType.byName(Args.string(args, "gamemode", "survival"), null);
        if (gameMode == null) throw new OpError("bad_args", "Unknown gamemode.", "Use survival, creative, adventure or spectator.");
        Difficulty difficulty = Difficulty.byName(Args.string(args, "difficulty", "normal"));
        if (difficulty == null) throw new OpError("bad_args", "Unknown difficulty.", "Use peaceful, easy, normal or hard.");
        return OnClient.call(() -> {
            Minecraft mc = Minecraft.getInstance();
            requireNoWorld(mc);
            if (mc.getLevelSource().levelExists(name)) {
                throw new OpError("world_exists", "A save named '" + name + "' already exists.", "Open it with world.open, or delete it with world.delete first.");
            }
            LevelSettings settings = new LevelSettings(name, gameMode, false, difficulty, true, testGameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(seed, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(FLAT).value().createWorldDimensions(),
                    new TitleScreen());
            return null;
        }).thenCompose(ignored -> awaitJoin(name)).thenCompose(result -> OnServer.call(server -> {
            // The pack's datapack worldgen makes the world "experimental"; NeoForge asks once on every reopen until
            // a player confirms. A test world counts as confirmed, so world.open never stops at that dialog.
            if (server.getWorldData() instanceof PrimaryLevelData data) data.withConfirmedWarning(true);
            return result;
        }));
    }

    /** Convention R2: day-night cycle, weather, mob spawning and phantoms off. A test that needs one enables it. */
    private static GameRules testGameRules() {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, null);
        return rules;
    }

    private static CompletableFuture<JsonElement> open(JsonObject args) {
        String name = Args.required(args, "name", "\"name\": \"e2e-flat-20261110\"").getAsString();
        return OnClient.call(() -> {
            Minecraft mc = Minecraft.getInstance();
            requireNoWorld(mc);
            if (!mc.getLevelSource().levelExists(name)) {
                throw new OpError("not_found", "No save named '" + name + "'.", "world.list shows the saves; world.create makes a new test world.");
            }
            mc.createWorldOpenFlows().openWorld(name, () -> mc.setScreen(new TitleScreen()));
            return null;
        }).thenCompose(ignored -> awaitJoin(name));
    }

    /**
     * Completes when the player is in the world with no screen open (the loading screens are gone). Fails early when
     * another screen (a confirmation or error dialog) waits for input instead of loading.
     */
    private static CompletableFuture<JsonElement> awaitJoin(String name) {
        int[] dialogTicks = {0};
        return TickTasks.onClientTicks(JOIN_TICKS, tick -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null && !isLoadingScreen(mc.screen)) {
                if (++dialogTicks[0] >= DIALOG_TICKS) {
                    throw new OpError("blocked_by_screen", "Opening '" + name + "' stopped at " + mc.screen.getClass().getName() + ": "
                            + mc.screen.getTitle().getString() + ".",
                            "A dialog waits for input. Check the client log; a test world should never ask (see WorldOps.create).");
                }
                return null;
            }
            dialogTicks[0] = 0;
            if (mc.player == null || mc.level == null || mc.screen != null || mc.getSingleplayerServer() == null) return null;
            JsonObject result = new JsonObject();
            result.addProperty("name", name);
            return (JsonElement) result;
        }, () -> {
            Minecraft mc = Minecraft.getInstance();
            return new OpError("join_timeout", "The world '" + name + "' did not open within " + JOIN_TICKS / 20 + " s; screen: "
                    + (mc.screen == null ? "none" : mc.screen.getClass().getName()) + ".",
                    "Check the client log (logs/latest.log) for a world loading error.");
        }, () -> {});
    }

    private static boolean isLoadingScreen(Screen screen) {
        return screen instanceof GenericMessageScreen || screen instanceof LevelLoadingScreen || screen instanceof ReceivingLevelScreen
                || screen instanceof ProgressScreen;
    }

    /** Saves and quits to the title screen, like Save and Quit in the pause menu. Does nothing outside a world. */
    private static JsonElement leave() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            boolean local = mc.isLocalServer();
            mc.level.disconnect();
            if (local) mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
            else mc.disconnect();
            mc.setScreen(new TitleScreen());
        }
        return new JsonObject();
    }

    private static CompletableFuture<JsonElement> delete(JsonObject args) {
        String name = testWorldName(args);
        return OnClient.call(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (name.equals(currentWorld(mc))) {
                throw new OpError("world_open", "The save '" + name + "' is open.", "Leave it first with world.leave.");
            }
            if (!mc.getLevelSource().levelExists(name)) return new JsonObject();
            try (LevelStorageSource.LevelStorageAccess access = mc.getLevelSource().createAccess(name)) {
                access.deleteLevel();
            } catch (IOException e) {
                throw new OpError("delete_failed", "Could not delete '" + name + "': " + e, "Is the save open in another game? Close it and retry.");
            }
            return new JsonObject();
        });
    }

    private static JsonElement list() {
        JsonArray names = new JsonArray();
        try {
            for (LevelStorageSource.LevelDirectory dir : Minecraft.getInstance().getLevelSource().findLevelCandidates()) {
                names.add(dir.directoryName());
            }
        } catch (LevelStorageException e) {
            throw new OpError("list_failed", "Could not list the saves: " + e.getMessage(), "Check the saves folder of the e2e instance.");
        }
        JsonObject result = new JsonObject();
        result.add("names", names);
        return result;
    }

    /** Folder name of the open save, or null. Client thread. */
    static String currentWorld(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return null;
        return server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
    }

    private static String testWorldName(JsonObject args) {
        String name = Args.required(args, "name", "\"name\": \"e2e-flat-20261110\"").getAsString();
        if (!name.startsWith(PREFIX) || !name.matches("[A-Za-z0-9_.-]+")) {
            throw new OpError("bad_args", "Test world names start with '" + PREFIX + "' and use only letters, digits, '-', '_' and '.'; got '" + name + "'.",
                    "Name it like e2e-flat-<run id>: the prefix keeps player worlds safe from world.delete.");
        }
        return name;
    }

    private static void requireNoWorld(Minecraft mc) {
        // Minecraft.doWorldLoad waits until the overlay is gone, but the overlay only finishes while that loop is
        // not running: opening a world during the startup reload hangs the client for good.
        if (mc.getOverlay() != null) {
            throw new OpError("not_ready", "The game is still loading resources.", "Wait until state.loading is false; npm run e2e waits for it before running specs.");
        }
        if (mc.level != null) {
            throw new OpError("in_world", "A world is already open: " + currentWorld(mc) + ".", "Leave it first with world.leave.");
        }
    }
}
