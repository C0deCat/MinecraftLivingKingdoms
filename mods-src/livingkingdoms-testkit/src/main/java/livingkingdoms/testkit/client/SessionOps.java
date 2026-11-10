package livingkingdoms.testkit.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import livingkingdoms.testkit.LivingKingdomsTestkit;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;

/** Session ops: hello, ping, state (screen, world, tick), quit. */
final class SessionOps {
    /** File-name prefix of our own JARs; their hashes let the launcher decide whether a running client is current. */
    private static final String OWN_JAR_PREFIX = "livingkingdoms_";

    private SessionOps() {}

    static void register(OpRegistry registry) {
        registry.register("hello", args -> CompletableFuture.completedFuture(hello(registry)))
                .register("ping", args -> CompletableFuture.completedFuture(new JsonObject()))
                .register("state", args -> OnClient.call(SessionOps::state))
                .register("quit", args -> {
                    // Reply first, then stop: the response must leave the socket before the JVM exits.
                    CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS)
                            .execute(() -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop()));
                    return CompletableFuture.completedFuture(new JsonObject());
                });
    }

    private static JsonElement hello(OpRegistry registry) {
        JsonObject result = new JsonObject();
        result.addProperty("protocol", LivingKingdomsTestkit.PROTOCOL_VERSION);
        result.addProperty("minecraft", SharedConstants.getCurrentVersion().getName());
        result.addProperty("neoforge", NeoForgeVersion.getVersion());
        JsonArray ops = new JsonArray();
        registry.names().forEach(ops::add);
        result.add("ops", ops);
        JsonObject ownJars = new JsonObject();
        JsonArray mods = new JsonArray();
        ModList.get().forEachModFile(file -> {
            Path path = file.getFilePath();
            String name = path.getFileName().toString();
            if (name.startsWith(OWN_JAR_PREFIX) && name.endsWith(".jar") && Files.isRegularFile(path)) {
                ownJars.addProperty(name, sha256(path));
            }
        });
        ModList.get().getMods().forEach(mod -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", mod.getModId());
            entry.addProperty("version", mod.getVersion().toString());
            mods.add(entry);
        });
        result.add("ownJars", ownJars);
        result.add("mods", mods);
        return result;
    }

    private static JsonElement state() {
        Minecraft mc = Minecraft.getInstance();
        JsonObject result = new JsonObject();
        result.addProperty("screen", mc.screen == null ? null : mc.screen.getClass().getName());
        ClientLevel level = mc.level;
        result.addProperty("inWorld", level != null && mc.player != null);
        result.add("tick", level == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(level.getGameTime()));
        result.addProperty("dimension", level == null ? null : level.dimension().location().toString());
        result.addProperty("paused", mc.isPaused());
        // The resource-reload overlay is up during startup; worlds must not be opened until it is gone.
        result.addProperty("loading", mc.getOverlay() != null);
        result.addProperty("world", WorldOps.currentWorld(mc));
        return result;
    }

    private static String sha256(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1 << 16];
            for (int read; (read = in.read(buffer)) > 0; ) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            return "unreadable: " + e;
        }
    }
}
