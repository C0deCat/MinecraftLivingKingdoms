package livingkingdoms.testkit.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

/**
 * Localhost socket of the e2e driver. One JSON object per line in both directions:
 * request {@code {"id":1,"op":"hello","args":{}}}, response {@code {"id":1,"ok":true,"result":...}} or
 * {@code {"id":1,"ok":false,"error":{"code":"...","message":"...","hint":"..."}}}.
 *
 * <p>Socket threads only parse and write; every op moves its work to the client or server thread itself.
 * Requests on one connection may complete out of order; the harness matches them by id.
 */
public final class Bridge {
    public static final int DEFAULT_PORT = 25598;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final AtomicInteger CONNECTIONS = new AtomicInteger();

    private final OpRegistry ops;
    private final int port;

    public Bridge(OpRegistry ops, int port) {
        this.ops = ops;
        this.port = port;
    }

    public void start() {
        Thread acceptor = new Thread(this::acceptLoop, "testkit-bridge");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private void acceptLoop() {
        try (ServerSocket server = new ServerSocket(port, 4, InetAddress.getLoopbackAddress())) {
            LOGGER.info("Testkit e2e driver listening on 127.0.0.1:{}", port);
            while (true) {
                Socket socket = server.accept();
                Thread connection = new Thread(() -> serve(socket), "testkit-conn-" + CONNECTIONS.incrementAndGet());
                connection.setDaemon(true);
                connection.start();
            }
        } catch (IOException e) {
            LOGGER.error("Testkit e2e driver could not listen on 127.0.0.1:{}; e2e tests cannot connect. "
                    + "Is another game instance running? Set -Dlivingkingdoms.testkit.port to change the port.", port, e);
        }
    }

    private void serve(Socket socket) {
        try (socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                handle(line, out);
            }
        } catch (IOException e) {
            LOGGER.debug("Testkit connection closed: {}", e.toString());
        }
    }

    private void handle(String line, Writer out) {
        JsonElement id = JsonNull.INSTANCE;
        CompletableFuture<JsonElement> result;
        try {
            JsonObject request = JsonParser.parseString(line).getAsJsonObject();
            id = request.has("id") ? request.get("id") : JsonNull.INSTANCE;
            String name = request.get("op").getAsString();
            JsonObject args = request.has("args") && request.get("args").isJsonObject() ? request.getAsJsonObject("args") : new JsonObject();
            Op op = ops.get(name);
            if (op == null) {
                throw new OpError("unknown_op", "The driver does not implement op '" + name + "'.",
                        "Declare the op in tests/e2e/harness/protocol.ts and implement it in livingkingdoms.testkit.client, then rebuild.");
            }
            result = op.run(args);
        } catch (RuntimeException e) {
            result = CompletableFuture.failedFuture(e);
        }
        JsonElement requestId = id;
        result.whenComplete((value, error) -> write(out, error == null ? success(requestId, value) : failure(requestId, error)));
    }

    private static JsonObject success(JsonElement id, JsonElement value) {
        JsonObject response = new JsonObject();
        response.add("id", id);
        response.addProperty("ok", true);
        response.add("result", value == null ? JsonNull.INSTANCE : value);
        return response;
    }

    private static JsonObject failure(JsonElement id, Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        JsonObject body = new JsonObject();
        if (cause instanceof OpError opError) {
            body.addProperty("code", opError.code());
            body.addProperty("message", opError.getMessage());
            body.addProperty("hint", opError.hint());
        } else {
            LOGGER.error("Testkit op failed", cause);
            body.addProperty("code", "internal");
            body.addProperty("message", String.valueOf(cause));
            body.addProperty("hint", "An unexpected exception in the driver; the stack trace is in the client log (logs/latest.log).");
        }
        JsonObject response = new JsonObject();
        response.add("id", id);
        response.addProperty("ok", false);
        response.add("error", body);
        return response;
    }

    private static void write(Writer out, JsonObject response) {
        synchronized (out) {
            try {
                out.write(GSON.toJson(response));
                out.write('\n');
                out.flush();
            } catch (IOException e) {
                LOGGER.debug("Testkit could not send a response: {}", e.toString());
            }
        }
    }
}
