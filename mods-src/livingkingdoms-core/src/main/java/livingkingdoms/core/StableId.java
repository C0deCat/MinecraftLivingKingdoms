package livingkingdoms.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Identity of a simulated thing (a resident, later a settlement or a place) that stays the same
 * across reloads and does not need a loaded entity or chunk. Given out by {@link CoreSaveData}.
 *
 * <p>Written as {@code kind:number}, for example {@code resident:17}.
 */
public record StableId(String kind, long number) {
    public static final Codec<StableId> CODEC = Codec.STRING.comapFlatMap(StableId::parse, StableId::toString);

    public StableId {
        if (kind.isEmpty() || kind.indexOf(':') >= 0) throw new IllegalArgumentException("Bad stable ID kind: " + kind);
        if (number <= 0) throw new IllegalArgumentException("Stable ID numbers start at 1: " + number);
    }

    public static DataResult<StableId> parse(String text) {
        int colon = text.indexOf(':');
        if (colon <= 0) return DataResult.error(() -> "Not a stable ID (kind:number): " + text);
        try {
            return DataResult.success(new StableId(text.substring(0, colon), Long.parseLong(text.substring(colon + 1))));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Not a stable ID (kind:number): " + text);
        }
    }

    @Override
    public String toString() {
        return kind + ":" + number;
    }
}
