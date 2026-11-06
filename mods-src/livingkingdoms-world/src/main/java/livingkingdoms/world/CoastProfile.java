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
 * Rays that start on land (no ocean at the border) are counted apart. The result is logged as
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

    /** One ray: coast distance from the border, or -1 if the ray starts on land or never finds land. */
    private record Ray(int coast, int rise, boolean beach, boolean landAtBorder) {}

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
            if (height(generator, level, random, px, pz) > SEA_LEVEL) return new Ray(-1, 0, false, true);
            for (int t = STEP; t < length; t += STEP) {
                double x = px + dx * t;
                double z = pz + dz * t;
                if (height(generator, level, random, x, z) <= SEA_LEVEL) continue;
                int rise = 0;
                for (int s = 0; s <= PROFILE; s += STEP) {
                    rise = Math.max(rise, height(generator, level, random, x + dx * s, z + dz * s) - SEA_LEVEL);
                }
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock((int) x), QuartPos.fromBlock(SEA_LEVEL), QuartPos.fromBlock((int) z), random.sampler());
                // Distance from the border along the axis the ray started from.
                int coast = (int) Math.round(half - Math.max(Math.abs(x), Math.abs(z)));
                return new Ray(coast, rise, biome.is(BiomeTags.IS_BEACH), false);
            }
            return new Ray(-1, 0, false, false);
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
        String message = String.format(Locale.ROOT,
                "Coast %s: %d rays, %d land at border, %d coasts; gentle %.0f%%, steep %.0f%%, cliff %.0f%%, beach biome %.0f%%; "
                        + "coast distance from border p10 %d / p50 %d / p90 %d; %d ms",
                size.id(), rays, landAtBorder, found, pct(gentle, found), pct(steep, found), pct(cliff, found), pct(beach, found),
                percentile(coasts, 0.1), percentile(coasts, 0.5), percentile(coasts, 0.9), System.currentTimeMillis() - started);
        LivingKingdomsWorld.LOGGER.info(message);
        source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(message), false));
    }

    private static int height(ChunkGenerator generator, ServerLevel level, RandomState random, double x, double z) {
        return generator.getBaseHeight((int) Math.floor(x), (int) Math.floor(z), Heightmap.Types.OCEAN_FLOOR_WG, level, random);
    }

    private static double pct(int part, int whole) {
        return whole == 0 ? 0 : 100.0 * part / whole;
    }

    private static int percentile(int[] sorted, double p) {
        if (sorted.length == 0) return -1;
        return sorted[Math.min(sorted.length - 1, (int) (p * sorted.length))];
    }
}
