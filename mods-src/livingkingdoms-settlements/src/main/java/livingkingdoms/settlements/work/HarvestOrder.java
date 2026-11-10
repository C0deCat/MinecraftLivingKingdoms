package livingkingdoms.settlements.work;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/**
 * A standing order of the M4-A prototype: harvest ripe crops within {@code radius} blocks of
 * {@code field} and deliver the harvest to the container at {@code storage}. S1 replaces it with
 * profession tasks.
 */
public record HarvestOrder(BlockPos field, int radius, BlockPos storage) {
    public static final int DEFAULT_RADIUS = 6;
    public static final int MAX_RADIUS = 16;

    public static final Codec<HarvestOrder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("field").forGetter(HarvestOrder::field),
            Codec.intRange(1, MAX_RADIUS).fieldOf("radius").forGetter(HarvestOrder::radius),
            BlockPos.CODEC.fieldOf("storage").forGetter(HarvestOrder::storage)
    ).apply(instance, HarvestOrder::new));
}
