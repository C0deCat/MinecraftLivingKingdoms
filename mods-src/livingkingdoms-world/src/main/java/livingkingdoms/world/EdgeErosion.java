package livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code livingkingdoms_world:edge_erosion}: lifts the generator's erosion towards
 * {@code target} near the world border, on the stretches of coast that {@code mask} selects
 * (ADR-0004).
 *
 * <p>Tectonic raises mountains where erosion is low, right up to a narrow continentalness window at
 * the waterline. Where the edge bias sinks such land, the mountains end in a wall. Higher erosion
 * turns that land into lowland, so the coast gets beaches and gentle slopes. The mask (a 2D noise,
 * roughly -1..1) keeps the natural erosion on other stretches, so some coasts stay mountainous.
 *
 * <p>The sign of {@code input} is kept; only its magnitude is raised, never lowered. The lift is
 * full from {@code open_ocean} blocks from the border up to half the band and fades out at
 * {@code start} blocks (at most {@code max_share} of the half-side), using the same distance as
 * {@link EdgeBias}.
 */
public record EdgeErosion(DensityFunction input, DensityFunction mask, int start, double maxShare, int openOcean,
                          double target, double maskLow, double maskHigh) implements DensityFunction {
    public static final MapCodec<EdgeErosion> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(EdgeErosion::input),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("mask").forGetter(EdgeErosion::mask),
            Codec.intRange(16, 8192).fieldOf("start").forGetter(EdgeErosion::start),
            Codec.doubleRange(0.05, 1.0).fieldOf("max_share").forGetter(EdgeErosion::maxShare),
            Codec.intRange(0, 4096).fieldOf("open_ocean").forGetter(EdgeErosion::openOcean),
            Codec.doubleRange(0, 1).fieldOf("target").forGetter(EdgeErosion::target),
            Codec.doubleRange(-2, 2).fieldOf("mask_low").forGetter(EdgeErosion::maskLow),
            Codec.doubleRange(-2, 2).fieldOf("mask_high").forGetter(EdgeErosion::maskHigh)
    ).apply(instance, EdgeErosion::new));
    public static final KeyDispatchDataCodec<EdgeErosion> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final boolean DISABLED = "off".equals(System.getProperty("livingkingdoms.edge"));

    @Override
    public double compute(FunctionContext context) {
        double erosion = input.compute(context);
        if (DISABLED) return erosion;
        int side = WorldSizeState.current().side();
        double half = side / 2.0;
        double toEdge = EdgeFalloff.distanceToEdge(context.blockX(), context.blockZ(), half);
        double from = Math.min(start, half * maxShare);
        if (toEdge >= from) return erosion;
        double open = Math.min(openOcean, side / 12.0);
        double near = Mth.clamp((from - toEdge) / ((from - open) * 0.5), 0.0, 1.0);
        double selected = Mth.clamp((mask.compute(context) - maskLow) / (maskHigh - maskLow), 0.0, 1.0);
        double lift = smooth(near) * smooth(selected);
        double magnitude = Math.abs(erosion);
        if (lift <= 0.0 || magnitude >= target) return erosion;
        double lifted = magnitude + (target - magnitude) * lift;
        return erosion < 0 ? -lifted : lifted;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new EdgeErosion(input.mapAll(visitor), mask.mapAll(visitor), start, maxShare, openOcean,
                target, maskLow, maskHigh));
    }

    @Override
    public double minValue() {
        return Math.min(input.minValue(), -target);
    }

    @Override
    public double maxValue() {
        return Math.max(input.maxValue(), target);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
