package livingkingdoms.settlements.settlement;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Removes what burns an adopted village down (TD-51): lava with air above it within
 * {@link #REACH} blocks of a building, and fire there. A small pool of lava inside the box of a building stays:
 * that is the forge of a smithy. Source lava hardens to obsidian, flowing lava to cobblestone.
 *
 * <p>Our own layout (slice S2) avoids such sites; this is only for villages the world generated.
 */
public final class SiteSafety {
    /** Lava sets fire to burnable blocks a few blocks away; this distance keeps buildings out of its reach. */
    public static final int REACH = 4;

    /** The forge of a vanilla smithy holds two lava blocks. */
    private static final int FORGE_MAX_BLOCKS = 4;

    private SiteSafety() {}

    /**
     * @param buildings the boxes of the buildings of the settlement
     * @param area      the part of the world to work in (a chunk column, or a test plot); must be loaded
     * @return the number of blocks changed
     */
    public static int clear(ServerLevel level, List<BoundingBox> buildings, BoundingBox area) {
        // Decide first, change afterwards: a lake that is hardened block by block would shrink into something that
        // looks like a forge.
        Map<BlockPos, BlockState> changes = new LinkedHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (BoundingBox building : buildings) {
            BoundingBox near = building.inflatedBy(REACH);
            if (!near.intersects(area)) continue;
            int minX = Math.max(near.minX(), area.minX());
            int maxX = Math.min(near.maxX(), area.maxX());
            int minY = Math.max(near.minY(), area.minY());
            int maxY = Math.min(near.maxY(), area.maxY());
            int minZ = Math.max(near.minZ(), area.minZ());
            int maxZ = Math.min(near.maxZ(), area.maxZ());
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.is(BlockTags.FIRE)) {
                            changes.put(pos.immutable(), Blocks.AIR.defaultBlockState());
                        } else if (state.getFluidState().is(FluidTags.LAVA) && level.getBlockState(above.set(x, y + 1, z)).isAir()
                                && !isForge(level, buildings, pos)) {
                            Block hardened = state.getFluidState().isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
                            changes.put(pos.immutable(), hardened.defaultBlockState());
                        }
                    }
                }
            }
        }
        changes.forEach((where, state) -> level.setBlock(where, state, Block.UPDATE_ALL));
        return changes.size();
    }

    /** A forge is a small pool of lava inside the box of a building; a lake that a building's box reaches into is not. */
    private static boolean isForge(ServerLevel level, List<BoundingBox> buildings, BlockPos pos) {
        boolean inside = false;
        for (BoundingBox building : buildings) {
            if (building.isInside(pos)) {
                inside = true;
                break;
            }
        }
        if (!inside) return false;
        LongSet pool = new LongOpenHashSet();
        Deque<BlockPos> open = new ArrayDeque<>();
        pool.add(pos.asLong());
        open.add(pos.immutable());
        while (!open.isEmpty()) {
            BlockPos current = open.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (level.getFluidState(next).is(FluidTags.LAVA) && pool.add(next.asLong())) {
                    if (pool.size() > FORGE_MAX_BLOCKS) return false;
                    open.add(next);
                }
            }
        }
        return true;
    }
}
