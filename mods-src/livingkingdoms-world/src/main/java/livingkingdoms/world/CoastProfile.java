package livingkingdoms.world;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * {@code /lkworld coast [rays]}: measures the coast that faces the world border, straight from the
 * generator noise. Rays start on the border, evenly spread along it, and walk towards the center
 * until they reach land. For each ray it records how far from the border the coast is and how
 * steeply the land rises in the first blocks inland.
 *
 * <p>Classes, by the highest ground within {@value #PROFILE} blocks inland of the waterline:
 * gentle (under {@value #STEEP} blocks above sea level), steep, and cliff ({@value #CLIFF}+).
 * Rays that start on land (no ocean at the border) are counted apart. Variety is reported as
 * p10/p50/p90 spreads (shore width, ground inland, sea floor offshore) and as the share of the most
 * common block-by-block shape of the first blocks inland. The result is logged as
 * {@code Coast <size>: ...}.
 */
final class CoastProfile {
    private static final int SEA_LEVEL = 63;
    private static final int STEP = 2;
    private static final int PROFILE = 8;
    private static final int STEEP = 5;
    private static final int CLIFF = 10;

    private CoastProfile() {}

    static int start(CommandSourceStack source, int rays) {
        WorldSize size = WorldSizeState.current();
        source.sendSuccess(() -> Component.literal("Profiling the coast of " + size.id() + " with " + rays + " rays in the background..."), false);
        Thread thread = new Thread(() -> {
            try {
                run(source, size, rays);
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("Coast profile failed", e);
            }
        }, "livingkingdoms-coast");
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    /**
     * One ray: coast distance from the border (-1 if the ray starts on land or never finds land),
     * the highest ground within {@value #PROFILE} blocks inland, the ground 16 and 64 blocks inland
     * and the sea floor 4 and 16 blocks out (relative to sea level), the shore width (blocks until the
     * ground is 3 above sea level) and the block-by-block shape of the first {@value #PROFILE} blocks.
     */
    private record Ray(int coast, int rise, int rise16, int rise64, int floor4, int floor16, int shore, String shape,
                       int gradient, int erosion, boolean beach, boolean landAtBorder) {
        static Ray none(boolean landAtBorder) {
            return new Ray(-1, 0, 0, 0, 0, 0, 0, "", 0, 0, false, landAtBorder);
        }
    }

    private static void run(CommandSourceStack source, WorldSize size, int rays) {
        ServerLevel level = source.getServer().overworld();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int half = size.side() / 2;
        long started = System.currentTimeMillis();

        Ray[] results = IntStream.range(0, rays).parallel().mapToObj(i -> {
            // A point on the square perimeter, 2 blocks inside the border, walking towards the center.
            double along = (double) i / rays * 4.0;
            int edge = (int) along;
            double f = along - edge;
            double px, pz;
            double inner = half - 2;
            switch (edge) {
                case 0 -> { px = -inner + 2 * inner * f; pz = -inner; }
                case 1 -> { px = inner; pz = -inner + 2 * inner * f; }
                case 2 -> { px = inner - 2 * inner * f; pz = inner; }
                default -> { px = -inner; pz = inner - 2 * inner * f; }
            }
            double length = Math.sqrt(px * px + pz * pz);
            double dx = -px / length;
            double dz = -pz / length;
            if (height(generator, level, random, px, pz) > SEA_LEVEL) return Ray.none(true);
            for (int t = STEP; t < length; t += STEP) {
                if (height(generator, level, random, px + dx * t, pz + dz * t) <= SEA_LEVEL) continue;
                // Step back to the exact first land block.
                if (height(generator, level, random, px + dx * (t - 1), pz + dz * (t - 1)) > SEA_LEVEL) t--;
                double x = px + dx * t;
                double z = pz + dz * t;
                int rise = 0;
                StringBuilder shape = new StringBuilder();
                for (int s = 0; s <= PROFILE; s++) {
                    int h = height(generator, level, random, x + dx * s, z + dz * s) - SEA_LEVEL;
                    rise = Math.max(rise, h);
                    shape.append(s == 0 ? "" : ",").append(Math.max(-9, Math.min(9, h)));
                }
                int shore = 0;
                while (shore < 64 && height(generator, level, random, x + dx * shore, z + dz * shore) - SEA_LEVEL < 3) shore++;
                int rise16 = height(generator, level, random, x + dx * 16, z + dz * 16) - SEA_LEVEL;
                int rise64 = height(generator, level, random, x + dx * 64, z + dz * 64) - SEA_LEVEL;
                int floor4 = height(generator, level, random, x - dx * 4, z - dz * 4) - SEA_LEVEL;
                int floor16 = height(generator, level, random, x - dx * 16, z - dz * 16) - SEA_LEVEL;
                // Continentalness change per block across the waterline (x 10^-5), and erosion at the coast (x 100).
                DensityFunction continents = random.router().continents();
                double cIn = continents.compute(new DensityFunction.SinglePointContext((int) (x + dx * 8), 0, (int) (z + dz * 8)));
                double cOut = continents.compute(new DensityFunction.SinglePointContext((int) (x - dx * 8), 0, (int) (z - dz * 8)));
                int gradient = (int) Math.round((cIn - cOut) / 16.0 * 1e5);
                int erosion = (int) Math.round(random.router().erosion().compute(new DensityFunction.SinglePointContext((int) x, 0, (int) z)) * 100);
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock((int) x), QuartPos.fromBlock(SEA_LEVEL), QuartPos.fromBlock((int) z), random.sampler());
                // Distance from the border along the axis the ray started from.
                int coast = (int) Math.round(half - Math.max(Math.abs(x), Math.abs(z)));
                return new Ray(coast, rise, rise16, rise64, floor4, floor16, shore, shape.toString(), gradient, erosion, biome.is(BiomeTags.IS_BEACH), false);
            }
            return Ray.none(false);
        }).toArray(Ray[]::new);

        int landAtBorder = 0, gentle = 0, steep = 0, cliff = 0, beach = 0;
        int[] coasts = Arrays.stream(results).filter(r -> r.coast() >= 0).mapToInt(Ray::coast).sorted().toArray();
        for (Ray ray : results) {
            if (ray.landAtBorder()) { landAtBorder++; continue; }
            if (ray.coast() < 0) continue;
            if (ray.rise() >= CLIFF) cliff++;
            else if (ray.rise() >= STEEP) steep++;
            else gentle++;
            if (ray.beach()) beach++;
        }
        int found = coasts.length;
        Ray[] coastRays = Arrays.stream(results).filter(r -> r.coast() >= 0).toArray(Ray[]::new);
        // Variety: how many rays share the most common block-by-block shape of the first blocks inland.
        java.util.Map<String, Long> shapes = Arrays.stream(coastRays)
                .collect(java.util.stream.Collectors.groupingBy(Ray::shape, java.util.stream.Collectors.counting()));
        java.util.Map.Entry<String, Long> topShape = shapes.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue()).orElse(java.util.Map.entry("-", 0L));
        String message = String.format(Locale.ROOT,
                "Coast %s: %d rays, %d land at border, %d coasts; gentle %.0f%%, steep %.0f%%, cliff %.0f%%, beach biome %.0f%%; "
                        + "coast distance from border p10 %d / p50 %d / p90 %d; "
                        + "shore width %s; ground +16 %s; +64 %s; sea floor -4 %s; -16 %s; "
                        + "continentalness per block x1e-5 %s; erosion x100 %s; %d shapes, top [%s] %.0f%%; %d ms",
                size.id(), rays, landAtBorder, found, pct(gentle, found), pct(steep, found), pct(cliff, found), pct(beach, found),
                percentile(coasts, 0.1), percentile(coasts, 0.5), percentile(coasts, 0.9),
                spread(coastRays, Ray::shore), spread(coastRays, Ray::rise16), spread(coastRays, Ray::rise64),
                spread(coastRays, Ray::floor4), spread(coastRays, Ray::floor16),
                spread(coastRays, Ray::gradient), spread(coastRays, Ray::erosion),
                shapes.size(), topShape.getKey(), pct(topShape.getValue().intValue(), found), System.currentTimeMillis() - started);
        LivingKingdomsWorld.LOGGER.info(message);
        source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(message), false));
    }

    private static int height(ChunkGenerator generator, ServerLevel level, RandomState random, double x, double z) {
        return generator.getBaseHeight((int) Math.floor(x), (int) Math.floor(z), Heightmap.Types.OCEAN_FLOOR_WG, level, random);
    }

    /** {@code p10/p50/p90} of one ray field. */
    private static String spread(Ray[] rays, java.util.function.ToIntFunction<Ray> field) {
        int[] values = Arrays.stream(rays).mapToInt(field).sorted().toArray();
        return percentile(values, 0.1) + "/" + percentile(values, 0.5) + "/" + percentile(values, 0.9);
    }

    private static double pct(int part, int whole) {
        return whole == 0 ? 0 : 100.0 * part / whole;
    }

    private static int percentile(int[] sorted, double p) {
        if (sorted.length == 0) return -1;
        return sorted[Math.min(sorted.length - 1, (int) (p * sorted.length))];
    }
}
