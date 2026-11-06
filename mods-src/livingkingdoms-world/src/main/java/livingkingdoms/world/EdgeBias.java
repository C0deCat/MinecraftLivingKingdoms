package livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code livingkingdoms_world:edge_bias}: a value <em>added</em> to continentalness
 * that sinks the land gradually towards the world border (ADR-0004).
 *
 * <p>Unlike the {@link EdgeFalloff} cap, an added bias keeps the shape of the generator's own noise:
 * the coast appears where {@code noise + bias} crosses the coast threshold, so bays, peninsulas,
 * beaches and slopes come from Tectonic and Terralith instead of following our contour.
 *
 * <p>It is 0 further than {@code start} blocks from the border (at most {@code max_share} of the
 * half-side) and eases down to {@code -depth} at the start of the open ocean ({@code open_ocean}
 * blocks from the border, at most a twelfth of the side). {@code jitter} (a 2D noise, roughly -1..1)
 * times {@code jitter_blocks} moves the whole profile in and out, so the slope is not the same
 * everywhere along the border.
 */
public record EdgeBias(DensityFunction jitter, int start, double maxShare, int openOcean, double jitterBlocks, double depth)
        implements DensityFunction {
    public static final MapCodec<EdgeBias> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("jitter").forGetter(EdgeBias::jitter),
            Codec.intRange(16, 8192).fieldOf("start").forGetter(EdgeBias::start),
            Codec.doubleRange(0.05, 1.0).fieldOf("max_share").forGetter(EdgeBias::maxShare),
            Codec.intRange(0, 4096).fieldOf("open_ocean").forGetter(EdgeBias::openOcean),
            Codec.doubleRange(0, 4096).fieldOf("jitter_blocks").forGetter(EdgeBias::jitterBlocks),
            Codec.doubleRange(0, 4).fieldOf("depth").forGetter(EdgeBias::depth)
    ).apply(instance, EdgeBias::new));
    public static final KeyDispatchDataCodec<EdgeBias> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final boolean DISABLED = "off".equals(System.getProperty("livingkingdoms.edge"));

    @Override
    public double compute(FunctionContext context) {
        if (DISABLED) return 0.0;
        int side = WorldSizeState.current().side();
        double half = side / 2.0;
        double toEdge = EdgeFalloff.distanceToEdge(context.blockX(), context.blockZ(), half);
        double open = Math.min(openOcean, side / 12.0);
        double from = Math.min(start, half * maxShare);
        double span = from - open;
        double shifted = toEdge + jitter.compute(context) * Math.min(jitterBlocks, span * 0.35);
        if (shifted >= from) return 0.0;
        double t = Mth.clamp((from - shifted) / span, 0.0, 1.0);
        // Smoothstep: no kink where the bias starts, steepest in the middle of the band.
        return -depth * t * t * (3.0 - 2.0 * t);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new EdgeBias(jitter.mapAll(visitor), start, maxShare, openOcean, jitterBlocks, depth));
    }

    @Override
    public double minValue() {
        return -depth;
    }

    @Override
    public double maxValue() {
        return 0.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
