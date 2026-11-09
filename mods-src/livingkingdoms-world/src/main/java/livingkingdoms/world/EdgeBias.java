package livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code livingkingdoms_world:edge_bias}: sinks the land of {@code input} (raw
 * continentalness) gradually towards the world border, keeping natural coast slopes (ADR-0004).
 *
 * <p><b>Bias.</b> A value is <em>added</em> to the input: 0 further than {@code start} blocks from the
 * border (at most {@code max_share} of the half-side), easing down to {@code -depth} at the start of
 * the open ocean ({@code open_ocean} blocks from the border, at most a twelfth of the side).
 * {@code jitter} (a 2D noise, roughly -1..1) times {@code jitter_blocks} moves where the fall starts;
 * it always ends at the open ocean. The coast appears where the generator's own noise plus the bias crosses the waterline, so
 * bays, capes and islands keep the generator's shapes.
 *
 * <p><b>Slope compensation.</b> The coast always forms where the bias falls fastest, so its fall adds
 * to the natural fall of the noise and makes shores and the sea floor next to them steeper than
 * anywhere inland. Inside a window around the coast value ({@code coast} minus
 * {@code window_below} to {@code coast} plus {@code window_above}) the result is compressed towards
 * {@code coast} by {@code k = natural / (natural + bias fall per block)}, at least
 * {@code min_compression}. The window then spans about as many blocks as without the bias. Values
 * outside the window are shifted so the function stays continuous and monotonic.
 *
 * <p><b>Shore styles.</b> {@code shore} varies the coast along the border. Where its {@code above}
 * noise is high the part of the window above the waterline is compressed further, down to
 * {@code above_min}: wide beaches and lowlands. Where its {@code below} noise is high the part below
 * is compressed, down to {@code below_min}: shallows and lagoons instead of an immediate drop.
 * Elsewhere the coast keeps the generator's own profile, so cliffs and steep shores remain.
 *
 * <p><b>Land lift.</b> With {@code land_lift} the input is first raised by the save's
 * {@link LandLift}, which guarantees a minimum land share for seeds that would be nearly all ocean.
 */
public record EdgeBias(DensityFunction input, DensityFunction jitter, int start, double maxShare, int openOcean,
                       double jitterBlocks, double depth, double coast, double windowBelow, double windowAbove,
                       double naturalGradient, double minCompression, Shore shore, boolean landLift) implements DensityFunction {
    /** Noise-selected shore styles: further compression above and below the waterline. */
    public record Shore(DensityFunction above, DensityFunction below, double aboveMin, double belowMin) {
        public static final Codec<Shore> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("above").forGetter(Shore::above),
                DensityFunction.HOLDER_HELPER_CODEC.fieldOf("below").forGetter(Shore::below),
                Codec.doubleRange(0.01, 1).fieldOf("above_min").forGetter(Shore::aboveMin),
                Codec.doubleRange(0.01, 1).fieldOf("below_min").forGetter(Shore::belowMin)
        ).apply(instance, Shore::new));

        Shore mapAll(Visitor visitor) {
            return new Shore(above.mapAll(visitor), below.mapAll(visitor), aboveMin, belowMin);
        }

        /** Factor for one side: 1 where the noise is below -0.2, {@code min} where it is above 0.2. */
        static double factor(DensityFunction noise, FunctionContext context, double min) {
            double selected = Mth.clamp((noise.compute(context) + 0.2) / 0.4, 0.0, 1.0);
            selected = selected * selected * (3.0 - 2.0 * selected);
            return 1.0 - (1.0 - min) * selected;
        }
    }

    public static final MapCodec<EdgeBias> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(EdgeBias::input),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("jitter").forGetter(EdgeBias::jitter),
            Codec.intRange(16, 8192).fieldOf("start").forGetter(EdgeBias::start),
            Codec.doubleRange(0.05, 1.0).fieldOf("max_share").forGetter(EdgeBias::maxShare),
            Codec.intRange(0, 4096).fieldOf("open_ocean").forGetter(EdgeBias::openOcean),
            Codec.doubleRange(0, 4096).fieldOf("jitter_blocks").forGetter(EdgeBias::jitterBlocks),
            Codec.doubleRange(0, 4).fieldOf("depth").forGetter(EdgeBias::depth),
            Codec.doubleRange(-2, 2).fieldOf("coast").forGetter(EdgeBias::coast),
            Codec.doubleRange(0, 2).fieldOf("window_below").forGetter(EdgeBias::windowBelow),
            Codec.doubleRange(0, 2).fieldOf("window_above").forGetter(EdgeBias::windowAbove),
            Codec.doubleRange(1e-6, 1).fieldOf("natural_gradient").forGetter(EdgeBias::naturalGradient),
            Codec.doubleRange(0.01, 1).fieldOf("min_compression").forGetter(EdgeBias::minCompression),
            Shore.CODEC.fieldOf("shore").forGetter(EdgeBias::shore),
            Codec.BOOL.optionalFieldOf("land_lift", false).forGetter(EdgeBias::landLift)
    ).apply(instance, EdgeBias::new));
    public static final KeyDispatchDataCodec<EdgeBias> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final boolean DISABLED = "off".equals(System.getProperty("livingkingdoms.edge"));

    @Override
    public double compute(FunctionContext context) {
        double value = input.compute(context);
        if (DISABLED) return value;
        if (landLift) value += LandLift.current();
        int side = WorldSizeState.current().side();
        double half = side / 2.0;
        double toEdge = EdgeFalloff.distanceToEdge(context.blockX(), context.blockZ(), half);
        double open = Math.min(openOcean, side / 12.0);
        double from = Math.min(start, half * maxShare);
        double reach = Math.min(jitterBlocks, (from - open) * 0.35);
        if (toEdge >= from + reach) return value;
        // The noise moves only where the fall starts; it always ends at the open ocean, so land never
        // reaches the cap that guarantees it.
        double begin = from + jitter.compute(context) * reach;
        double span = begin - open;
        if (toEdge >= begin) return value;
        double t = Mth.clamp((begin - toEdge) / span, 0.0, 1.0);
        // Smoothstep: no kink where the bias starts, steepest in the middle of the band.
        double biased = value - depth * t * t * (3.0 - 2.0 * t);
        double fall = depth * 6.0 * t * (1.0 - t) / span;
        double k = Math.max(minCompression, naturalGradient / (naturalGradient + fall));
        // Shore styles fade in over the first quarter of the band.
        double presence = Math.min(1.0, t * 4.0);
        double kAbove = k * (1.0 - presence * (1.0 - Shore.factor(shore.above(), context, shore.aboveMin())));
        double kBelow = k * (1.0 - presence * (1.0 - Shore.factor(shore.below(), context, shore.belowMin())));
        return compress(biased, kAbove, kBelow);
    }

    /**
     * Compresses the window around {@code coast} by {@code kAbove} above it and {@code kBelow} below
     * it; shifts the rest to stay continuous.
     */
    private double compress(double v, double kAbove, double kBelow) {
        double offset = v - coast;
        if (offset > windowAbove) return v - (1.0 - kAbove) * windowAbove;
        if (offset >= 0) return coast + kAbove * offset;
        if (offset < -windowBelow) return v + (1.0 - kBelow) * windowBelow;
        return coast + kBelow * offset;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new EdgeBias(input.mapAll(visitor), jitter.mapAll(visitor), start, maxShare, openOcean,
                jitterBlocks, depth, coast, windowBelow, windowAbove, naturalGradient, minCompression, shore.mapAll(visitor), landLift));
    }

    @Override
    public double minValue() {
        return input.minValue() - depth;
    }

    @Override
    public double maxValue() {
        return input.maxValue() + windowBelow + (landLift ? LandLift.MAX_LIFT : 0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
