package livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function {@code livingkingdoms_world:edge_falloff}: an upper cap for continentalness that
 * guarantees open ocean along the world border (ADR-0004).
 *
 * <p>It is {@code land} further than {@code ring} blocks (at most a sixth of the side) from the border and eases down to
 * {@code ocean} at the border. {@code jitter} (a 2D noise, roughly -1..1) times {@code jitter_blocks}
 * moves the line so it is not a straight square. A Lithostitched modifier applies it as
 * {@code min(continents, edge_falloff)}. The coast itself comes from {@link EdgeBias} and
 * {@link EdgeErosion}; this cap only acts on the outer band, where the bias has already sunk the land.
 *
 * <p>The border half-side comes from the save's world size, set before levels load
 * ({@link WorldSizeState}). With {@code -Dlivingkingdoms.edge=off} it returns {@code land}
 * everywhere, like the other edge functions (natural-coast baseline for {@code /lkworld coast}).
 */
public record EdgeFalloff(DensityFunction jitter, int ring, double jitterBlocks, double land, double ocean) implements DensityFunction {
    public static final MapCodec<EdgeFalloff> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("jitter").forGetter(EdgeFalloff::jitter),
            Codec.intRange(16, 8192).fieldOf("ring").forGetter(EdgeFalloff::ring),
            Codec.doubleRange(0, 4096).fieldOf("jitter_blocks").forGetter(EdgeFalloff::jitterBlocks),
            Codec.doubleRange(-2, 2).fieldOf("land").forGetter(EdgeFalloff::land),
            Codec.doubleRange(-2, 2).fieldOf("ocean").forGetter(EdgeFalloff::ocean)
    ).apply(instance, EdgeFalloff::new));
    public static final KeyDispatchDataCodec<EdgeFalloff> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final boolean DISABLED = "off".equals(System.getProperty("livingkingdoms.edge"));

    @Override
    public double compute(FunctionContext context) {
        if (DISABLED) return land;
        int side = WorldSizeState.current().side();
        double half = side / 2.0;
        // The ring never takes more than a sixth of the side, so the Test size keeps an inland core.
        double effectiveRing = Math.min(ring, side / 6.0);
        double toEdge = distanceToEdge(context.blockX(), context.blockZ(), half);
        // Outer half of the ring: open ocean. Inner half: the cap eases up to land, and the jitter
        // moves the coastline back and forth inside it, so coasts do not follow the square.
        double openOcean = effectiveRing * 0.5;
        double ramp = effectiveRing - openOcean;
        double shifted = toEdge + jitter.compute(context) * Math.min(jitterBlocks, ramp * 0.5);
        if (shifted >= effectiveRing) return land;
        double t = Mth.clamp((shifted - openOcean) / ramp, 0.0, 1.0);
        if (toEdge < openOcean) t = 0.0;
        double smooth = t * t * (3.0 - 2.0 * t);
        return ocean + (land - ocean) * smooth;
    }

    /**
     * Distance from a point to the border along a superellipse (p = 8): square overall, rounded
     * corners. 0 on the border, {@code half} at the center.
     */
    static double distanceToEdge(int x, int z, double half) {
        double nx = Math.abs(x) / half;
        double nz = Math.abs(z) / half;
        double distance = half * Math.pow(Math.pow(nx, 8) + Math.pow(nz, 8), 1.0 / 8.0);
        return half - distance;
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new EdgeFalloff(jitter.mapAll(visitor), ring, jitterBlocks, land, ocean));
    }

    @Override
    public double minValue() {
        return Math.min(land, ocean);
    }

    @Override
    public double maxValue() {
        return Math.max(land, ocean);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
