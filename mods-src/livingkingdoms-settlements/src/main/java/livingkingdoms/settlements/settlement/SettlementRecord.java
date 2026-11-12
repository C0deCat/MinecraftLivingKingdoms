package livingkingdoms.settlements.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import livingkingdoms.core.StableId;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A settlement: a place with residents. In slice S1 it is a village the world generated, adopted
 * when one of its chunks first loads (D-53); its buildings are the pieces of that structure.
 * Changed only through {@link Settlements}, on the server thread.
 */
public final class SettlementRecord {
    static final Codec<SettlementRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableId.CODEC.fieldOf("id").forGetter(SettlementRecord::id),
            Codec.STRING.fieldOf("source").forGetter(SettlementRecord::source),
            Codec.LONG.fieldOf("start_chunk").forGetter(SettlementRecord::startChunk),
            BoundingBox.CODEC.fieldOf("bounds").forGetter(SettlementRecord::bounds),
            Codec.LONG.listOf().optionalFieldOf("safe_chunks", List.of()).forGetter(record -> List.copyOf(record.safeChunks))
    ).apply(instance, SettlementRecord::new));

    private final StableId id;
    private final String source;
    private final long startChunk;
    private final BoundingBox bounds;
    private final LongSet safeChunks = new LongOpenHashSet();

    SettlementRecord(StableId id, String source, long startChunk, BoundingBox bounds, List<Long> safeChunks) {
        this.id = id;
        this.source = source;
        this.startChunk = startChunk;
        this.bounds = bounds;
        this.safeChunks.addAll(safeChunks);
    }

    public StableId id() {
        return id;
    }

    /** What the settlement came from: the ID of the generated structure. */
    public String source() {
        return source;
    }

    /** The chunk that holds the structure start, as {@code ChunkPos.toLong()}; one settlement per start. */
    public long startChunk() {
        return startChunk;
    }

    public BoundingBox bounds() {
        return bounds;
    }

    public BlockPos centre() {
        return bounds.getCenter();
    }

    public boolean contains(BlockPos pos) {
        return bounds.isInside(pos);
    }

    /** Whether the fire hazards of this chunk were already removed (TD-51). */
    boolean isSafe(long chunk) {
        return safeChunks.contains(chunk);
    }

    void markSafe(long chunk) {
        safeChunks.add(chunk);
    }
}
