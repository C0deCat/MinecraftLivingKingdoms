package livingkingdoms.world;

import java.util.List;
import java.util.Locale;
import livingkingdoms.world.StructureScan.Area;
import livingkingdoms.world.StructureScan.Start;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * {@code /lkworld grounding <structure> [radius]}: checks the placed blocks of a structure's
 * entrance (its start piece), unlike the census, which reads the generator noise. The chunks under
 * each entrance in the radius are fully generated; then every column whose lowest solid block lies
 * in the piece's bottom layer counts as hanging if the block under it is not ground. Used to verify
 * the {@code grounding} processor. Logs {@code Grounding <structure>: ...} with one line per start.
 */
final class GroundingCheck {
    private GroundingCheck() {}

    static int start(CommandSourceStack source, String structure, int radius) {
        ServerLevel level = source.getLevel();
        source.sendSuccess(() -> Component.literal("Checking the grounding of " + structure + " within " + radius + " blocks..."), false);
        Thread thread = new Thread(() -> {
            try {
                run(source, level, structure, radius);
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("Grounding check failed", e);
            }
        }, "livingkingdoms-grounding");
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    private static void run(CommandSourceStack source, ServerLevel level, String structure, int radius) {
        List<Start> starts = StructureScan.scan(level, Area.square(radius)).starts().stream()
                .filter(s -> s.id().equals(structure)).toList();
        int bottomTotal = 0;
        int hangingTotal = 0;
        StringBuilder lines = new StringBuilder();
        for (Start s : starts) {
            BoundingBox box = s.entranceBox();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, true);
                }
            }
            int[] counts = level.getServer().submit(() -> count(level, box)).join();
            bottomTotal += counts[0];
            hangingTotal += counts[1];
            lines.append(String.format(Locale.ROOT, "%n  (%d, %d, %d): %d of %d bottom columns hanging", box.getCenter().getX(), box.minY(),
                    box.getCenter().getZ(), counts[1], counts[0]));
        }
        String first = String.format(Locale.ROOT, "Grounding %s: %d starts within %d blocks, %d of %d bottom columns of the entrance hanging",
                structure, starts.size(), radius, hangingTotal, bottomTotal);
        LivingKingdomsWorld.LOGGER.info(first + lines);
        source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(first), false));
    }

    /** Bottom-layer columns of the box and how many of them have no ground under them. Server thread. */
    private static int[] count(ServerLevel level, BoundingBox box) {
        int bottom = 0;
        int hanging = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                ChunkAccess chunk = level.getChunk(x >> 4, z >> 4);
                cursor.set(x, box.minY(), z);
                BlockState state = chunk.getBlockState(cursor);
                if (state.isAir() || !state.blocksMotion()) continue;
                bottom++;
                cursor.move(0, -1, 0);
                BlockState below = chunk.getBlockState(cursor);
                if (!below.blocksMotion() || below.is(BlockTags.LEAVES)) hanging++;
            }
        }
        return new int[] {bottom, hanging};
    }
}
