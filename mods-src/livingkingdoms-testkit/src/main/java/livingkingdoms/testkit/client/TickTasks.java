package livingkingdoms.testkit.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Ops that span several ticks. Client tasks run at the start of every client tick (before input is read, so a
 * key held here moves the player in the same tick); server waits count integrated server ticks.
 */
public final class TickTasks {
    /** One step per client tick: return a result to finish, null to continue, or throw an OpError to fail. */
    @FunctionalInterface
    public interface Step<T> {
        T tick(int tick);
    }

    private record ClientTask<T>(Step<T> step, int maxTicks, Supplier<OpError> onTimeout, Runnable cleanup, CompletableFuture<T> future) {}

    private record ServerWait(long until, CompletableFuture<Long> future) {}

    private static final Queue<ClientTask<?>> INCOMING = new ConcurrentLinkedQueue<>();
    private static final List<ClientTask<?>> RUNNING = new ArrayList<>();
    private static final List<Integer> AGES = new ArrayList<>();
    private static final Queue<ServerWait> SERVER_WAITS = new ConcurrentLinkedQueue<>();
    private static final AtomicLong SERVER_TICKS = new AtomicLong();

    private TickTasks() {}

    static void register(IEventBus bus) {
        bus.addListener(ClientTickEvent.Pre.class, event -> tickClient());
        bus.addListener(ServerTickEvent.Post.class, event -> tickServer());
    }

    /**
     * Runs {@code step} once per client tick, from the next tick on, until it returns a value or {@code maxTicks}
     * pass. {@code cleanup} runs on the client thread whenever the task ends (success, failure or timeout).
     */
    public static <T> CompletableFuture<T> onClientTicks(int maxTicks, Step<T> step, Supplier<OpError> onTimeout, Runnable cleanup) {
        CompletableFuture<T> future = new CompletableFuture<>();
        INCOMING.add(new ClientTask<>(step, maxTicks, onTimeout, cleanup, future));
        return future;
    }

    /** Completes after {@code ticks} more integrated server ticks; fails when no world is open. */
    public static CompletableFuture<Long> serverTicks(int ticks) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) return CompletableFuture.failedFuture(OnServer.noWorld());
        CompletableFuture<Long> future = new CompletableFuture<>();
        SERVER_WAITS.add(new ServerWait(SERVER_TICKS.get() + ticks, future));
        return future;
    }

    private static void tickClient() {
        for (ClientTask<?> task; (task = INCOMING.poll()) != null; ) {
            RUNNING.add(task);
            AGES.add(0);
        }
        for (int i = RUNNING.size() - 1; i >= 0; i--) {
            ClientTask<?> task = RUNNING.get(i);
            int age = AGES.get(i);
            if (advance(task, age)) {
                RUNNING.remove(i);
                AGES.remove(i);
            } else {
                AGES.set(i, age + 1);
            }
        }
    }

    /** Returns true when the task has ended. */
    private static <T> boolean advance(ClientTask<T> task, int age) {
        try {
            if (age >= task.maxTicks()) {
                task.cleanup().run();
                task.future().completeExceptionally(task.onTimeout().get());
                return true;
            }
            T result = task.step().tick(age);
            if (result == null) return false;
            task.cleanup().run();
            task.future().complete(result);
        } catch (RuntimeException e) {
            try {
                task.cleanup().run();
            } finally {
                task.future().completeExceptionally(e);
            }
        }
        return true;
    }

    private static void tickServer() {
        long now = SERVER_TICKS.incrementAndGet();
        for (Iterator<ServerWait> it = SERVER_WAITS.iterator(); it.hasNext(); ) {
            ServerWait wait = it.next();
            if (now >= wait.until()) {
                it.remove();
                wait.future().complete(now);
            }
        }
    }
}
