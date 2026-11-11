package livingkingdoms.settlements.survey;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The walkable-layout measurement of M4: from the village centre, which doors can be reached on
 * foot with steps of at most one block up or down. The rule is symmetric, so a reached door can
 * also be walked back from.
 *
 * <p>A position is where the feet are: two passable blocks on a block that carries. Doors and
 * fence gates count as open; water and lava are not walked through.
 */
public final class Walkability {
    /** How far around the structure the walk may leave its bounding box. */
    static final int MARGIN = 12;
    private static final int VERTICAL_MARGIN = 6;
    private static final int GAP_RADIUS = 3;
    /** Height changes tried for a step to the next column: level first, then one up, then one down. */
    private static final int[] STEPS = {0, 1, -1};

    /** A door's lower half and how it relates to the reached area. */
    public record Door(BlockPos pos, boolean reached, int gap) {}

    public record Result(Optional<BlockPos> centre, boolean bell, List<Door> doors, int reachedCells) {
        public long reachedDoors() {
            return doors.stream().filter(Door::reached).count();
        }
    }

    private Walkability() {}

    public static Result measure(ServerLevel level, BoundingBox structure) {
        BoundingBox area = structure.inflatedBy(MARGIN, VERTICAL_MARGIN, MARGIN);
        List<BlockPos> doorPositions = new ArrayList<>();
        BlockPos bell = null;
        for (BlockPos pos : BlockPos.betweenClosed(structure.minX(), structure.minY(), structure.minZ(),
                structure.maxX(), structure.maxY(), structure.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.DOORS) && state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                doorPositions.add(pos.immutable());
            } else if (bell == null && state.is(Blocks.BELL)) {
                bell = pos.immutable();
            }
        }

        // The centre is the largest walkable area that touches the surroundings of the bell (or of the middle of the
        // structure without one). The nearest position alone is often the rim of the well or a roof, cut off from the street.
        Optional<BlockPos> centre = Optional.empty();
        LongSet reached = new LongOpenHashSet();
        for (BlockPos candidate : standableNear(level, bell != null ? bell : structure.getCenter(), bell != null ? 4 : 6)) {
            if (reached.contains(candidate.asLong())) continue;
            LongSet candidateArea = flood(level, area, candidate);
            if (candidateArea.size() > reached.size()) {
                reached = candidateArea;
                centre = Optional.of(candidate);
            }
        }

        List<Door> doors = new ArrayList<>();
        for (BlockPos door : doorPositions) {
            boolean hit = reached.contains(door.asLong());
            doors.add(new Door(door, hit, hit ? 0 : gap(reached, door)));
        }
        return new Result(centre, bell != null, doors, reached.size());
    }

    private static LongSet flood(ServerLevel level, BoundingBox area, BlockPos start) {
        LongSet reached = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        reached.add(start.asLong());
        queue.enqueue(start.asLong());
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            BlockPos current = BlockPos.of(queue.dequeueLong());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                int x = current.getX() + direction.getStepX();
                int z = current.getZ() + direction.getStepZ();
                if (x < area.minX() || x > area.maxX() || z < area.minZ() || z > area.maxZ()) continue;
                for (int dy : STEPS) {
                    int y = current.getY() + dy;
                    if (y < area.minY() || y > area.maxY()) continue;
                    next.set(x, y, z);
                    if (!standable(level, next)) continue;
                    // Headroom for the step: above the current head when going up, above the target's head when going down.
                    if (dy == 1 && !passable(level, current.above(2))) continue;
                    if (dy == -1 && !passable(level, next.above(2))) continue;
                    if (reached.add(next.asLong())) queue.enqueue(next.asLong());
                    break;
                }
            }
        }
        return reached;
    }

    /** Smallest height difference between an unreached door and a reached position near it, or -1 if none is near. */
    private static int gap(LongSet reached, BlockPos door) {
        int best = -1;
        for (int dx = -GAP_RADIUS; dx <= GAP_RADIUS; dx++) {
            for (int dz = -GAP_RADIUS; dz <= GAP_RADIUS; dz++) {
                for (int dy = -VERTICAL_MARGIN; dy <= VERTICAL_MARGIN; dy++) {
                    if (reached.contains(BlockPos.asLong(door.getX() + dx, door.getY() + dy, door.getZ() + dz))) {
                        int height = Math.abs(dy);
                        if (best < 0 || height < best) best = height;
                    }
                }
            }
        }
        return best;
    }

    /** Standable positions around a block and at most one above it, nearest first. */
    private static List<BlockPos> standableNear(ServerLevel level, BlockPos around, int radius) {
        List<BlockPos> found = new ArrayList<>();
        // Not above the block: the street is at the bell's level or below it, roofs and tree tops are above.
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-radius, -radius, -radius), around.offset(radius, 1, radius))) {
            if (standable(level, pos)) found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(around)));
        return found;
    }

    static boolean standable(ServerLevel level, BlockPos feet) {
        return passable(level, feet) && passable(level, feet.above()) && carries(level, feet.below());
    }

    private static boolean passable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.FENCE_GATES)) return true;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean carries(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) return false;
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.5;
    }
}
