package livingkingdoms.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Debug commands. {@code /lkworld info} reports the size and the current dimension border.
 * {@code /lkworld map [step]} renders the whole Overworld inside the border to a PNG
 * straight from the generator noise (no chunks are generated). Used to judge the world edge, the
 * ocean ring and the land share. Output: {@code <world>/livingkingdoms/map-<size>.png}.
 */
final class WorldMapCommand {
    private static final int SEA_LEVEL = 63;

    private WorldMapCommand() {}

    static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("lkworld")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info").executes(WorldMapCommand::info))
                .then(Commands.literal("coast")
                        .executes(context -> CoastProfile.start(context.getSource(), 720))
                        .then(Commands.argument("rays", IntegerArgumentType.integer(16, 8192))
                                .executes(context -> CoastProfile.start(context.getSource(), IntegerArgumentType.getInteger(context, "rays")))))
                .then(Commands.literal("map")
                        .executes(context -> render(context, 0))
                        .then(Commands.argument("step", IntegerArgumentType.integer(4, 256))
                                .executes(context -> render(context, IntegerArgumentType.getInteger(context, "step"))))));
    }

    /** {@code /lkworld info}: world size, and border and coordinate scale of the caller's dimension. */
    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        WorldSize size = WorldSizeState.current();
        String message = String.format(java.util.Locale.ROOT, "World size %s (%,d). %s: border %,d wide, coordinate_scale %s",
                size.id(), size.side(), level.dimension().location(), (int) level.getWorldBorder().getSize(),
                level.dimensionType().coordinateScale());
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    /** Starts rendering on a background thread: sampling the noise takes ~0.25 ms per point. */
    private static int render(CommandContext<CommandSourceStack> context, int requestedStep) {
        CommandSourceStack source = context.getSource();
        WorldSize size = WorldSizeState.current();
        int step = requestedStep > 0 ? requestedStep : Math.max(4, size.side() / 256);
        source.sendSuccess(() -> Component.literal("Rendering map of " + size.id() + " at " + step + " blocks/px in the background..."), false);
        Thread thread = new Thread(() -> {
            try {
                renderNow(source, size, step);
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("Map rendering failed", e);
            }
        }, "livingkingdoms-map");
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    private static void renderNow(CommandSourceStack source, WorldSize size, int step) {
        ServerLevel level = source.getServer().overworld();
        int half = size.side() / 2;
        int pixels = size.side() / step;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();

        BufferedImage image = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_RGB);
        java.util.concurrent.atomic.AtomicLong landCounter = new java.util.concurrent.atomic.AtomicLong();
        long started = System.currentTimeMillis();
        // Rows in parallel: noise sampling is thread-safe (vanilla worldgen samples it from worker threads).
        java.util.stream.IntStream.range(0, pixels).parallel().forEach(py -> {
            int[] row = new int[pixels];
            for (int px = 0; px < pixels; px++) {
                int x = -half + px * step + step / 2;
                int z = -half + py * step + step / 2;
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                        QuartPos.fromBlock(x), QuartPos.fromBlock(Math.max(height, SEA_LEVEL)), QuartPos.fromBlock(z), random.sampler());
                if (height >= SEA_LEVEL) landCounter.incrementAndGet();
                row[px] = color(height, biome);
            }
            synchronized (image) {
                image.setRGB(0, py, pixels, 1, row, 0, pixels);
            }
        });
        long land = landCounter.get();
        Path file = level.getServer().getWorldPath(LevelResource.ROOT).resolve("livingkingdoms").resolve("map-" + size.id() + ".png");
        try {
            Files.createDirectories(file.getParent());
            ImageIO.write(image, "png", file.toFile());
        } catch (IOException e) {
            LivingKingdomsWorld.LOGGER.error("Could not write map {}", file, e);
            return;
        }
        double landShare = 100.0 * land / ((long) pixels * pixels);
        String message = String.format(java.util.Locale.ROOT, "Map %s: %dx%d px, %d blocks/px, land %.1f%%, %d ms -> %s",
                size.id(), pixels, pixels, step, landShare, System.currentTimeMillis() - started, file.toAbsolutePath());
        LivingKingdomsWorld.LOGGER.info(message);
        source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(message), false));
    }

    private static int color(int height, Holder<Biome> biome) {
        if (height < SEA_LEVEL) {
            double depth = Math.min(1.0, (SEA_LEVEL - height) / 40.0);
            return rgb(30, (int) (110 - 70 * depth), (int) (200 - 90 * depth));
        }
        int base;
        if (biome.is(BiomeTags.IS_BEACH)) base = rgb(225, 210, 150);
        else if (biome.is(BiomeTags.IS_RIVER)) base = rgb(60, 120, 220);
        else if (biome.is(BiomeTags.IS_BADLANDS)) base = rgb(200, 110, 60);
        else if (biome.value().getBaseTemperature() < 0.15f) base = rgb(235, 240, 250);
        else if (biome.value().getBaseTemperature() > 1.5f) base = rgb(220, 200, 120);
        else if (biome.is(BiomeTags.IS_FOREST)) base = rgb(40, 120, 50);
        else base = rgb(100, 170, 70);
        double shade = 0.75 + Math.min(0.45, (height - SEA_LEVEL) / 400.0);
        if (height > 160) base = blend(base, rgb(140, 140, 140), Math.min(1.0, (height - 160) / 60.0));
        return scale(base, shade);
    }

    private static int rgb(int r, int g, int b) {
        return (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, b));
    }

    private static int blend(int a, int b, double t) {
        return rgb((int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t),
                (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t),
                (int) ((a & 255) * (1 - t) + (b & 255) * t));
    }

    private static int scale(int c, double s) {
        return rgb((int) (((c >> 16) & 255) * s), (int) (((c >> 8) & 255) * s), (int) ((c & 255) * s));
    }
}
