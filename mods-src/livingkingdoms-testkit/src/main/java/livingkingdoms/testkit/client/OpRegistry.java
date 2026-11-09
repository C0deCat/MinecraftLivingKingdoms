package livingkingdoms.testkit.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** All ops the driver implements, by name. `hello` reports the names so the harness can compare them with protocol.ts. */
public final class OpRegistry {
    private final Map<String, Op> ops = new LinkedHashMap<>();

    public OpRegistry register(String name, Op op) {
        if (ops.putIfAbsent(name, op) != null) throw new IllegalStateException("Op registered twice: " + name);
        return this;
    }

    public Op get(String name) {
        return ops.get(name);
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(ops.keySet());
    }
}
