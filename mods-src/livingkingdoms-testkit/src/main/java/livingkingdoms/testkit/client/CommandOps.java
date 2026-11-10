package livingkingdoms.testkit.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Runs a command on the integrated server thread with operator rights and returns its result and feedback.
 * For arrange() (convention A1); the behaviour under test goes through player and input ops.
 */
final class CommandOps {
    private static final int OPERATOR = 4;

    private CommandOps() {}

    static void register(OpRegistry registry) {
        registry.register("command.run", CommandOps::run);
    }

    private static CompletableFuture<JsonElement> run(JsonObject args) {
        String command = Args.required(args, "cmd", "\"cmd\": \"give @s minecraft:stone\"").getAsString();
        String as = Args.string(args, "as", "player");
        if (!as.equals("player") && !as.equals("server")) throw new OpError("bad_args", "Unknown source '" + as + "'.", "Use as: \"player\" or \"server\".");
        return OnServer.call(server -> run(server, command.startsWith("/") ? command.substring(1) : command, as));
    }

    private static JsonElement run(MinecraftServer server, String command, String as) {
        List<String> feedback = new ArrayList<>();
        boolean[] success = {false};
        boolean[] called = {false};
        int[] result = {0};
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                feedback.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack base = as.equals("player") ? OnServer.player(server).createCommandSourceStack() : server.createCommandSourceStack();
        CommandSourceStack source = base.withSource(capture).withPermission(OPERATOR).withCallback((ok, value) -> {
            called[0] = true;
            success[0] = ok;
            result[0] = value;
        });
        server.getCommands().performPrefixedCommand(source, command);
        if (!called[0] || !success[0]) {
            throw new OpError("command_failed", "/" + command + " failed" + (feedback.isEmpty() ? "." : ": " + String.join(" | ", feedback)),
                    "Check the 1.21.1 syntax (item stacks in NBT use id and count) and the coordinates. Some commands fail when they change nothing "
                            + "(fill, clear, kill with no target): pass allowFail in the harness for those.");
        }
        JsonObject json = new JsonObject();
        json.addProperty("result", result[0]);
        JsonArray lines = new JsonArray();
        feedback.forEach(lines::add);
        json.add("feedback", lines);
        return json;
    }
}
