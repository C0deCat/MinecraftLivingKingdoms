package livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * {@code livingkingdoms_world:grounding}: grounds the bottom layer of a rigid piece that hangs over
 * a slope. For every column whose lowest solid block lies in the piece's bottom layer and has no
 * ground under it, {@code extend} fills down to the ground with {@code block} (default: the column's
 * own bottom block) for at most {@code max_depth} blocks; a column that does not reach the ground
 * within that depth, and every hanging column in {@code drop} mode, is removed, so the ruin looks
 * broken off instead of floating.
 *
 * <p>Runs in {@link #finalizeProcessing}, after the other processors. It reads and writes only
 * columns inside the placement's bounding box, the chunk being generated, so it never touches a
 * chunk of the generation region that is not ready. Pieces placed with {@code terrain_matching}
 * (they carry a {@link GravityProcessor}) follow the ground already and are left alone.
 * Attached to other mods' pools with Lithostitched {@code set_pool_element_processors}.
 */
final class GroundingProcessor extends StructureProcessor {
    enum Mode implements StringRepresentable {
        EXTEND("extend"),
        DROP("drop");

        static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    static final MapCodec<GroundingProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Mode.CODEC.fieldOf("mode").forGetter(p -> p.mode),
            BlockState.CODEC.optionalFieldOf("block").forGetter(p -> p.block),
            Codec.intRange(1, 64).optionalFieldOf("max_depth", 16).forGetter(p -> p.maxDepth)
    ).apply(instance, GroundingProcessor::new));

    static final StructureProcessorType<GroundingProcessor> TYPE = () -> CODEC;

    private final Mode mode;
    private final Optional<BlockState> block;
    private final int maxDepth;

    GroundingProcessor(Mode mode, Optional<BlockState> block, int maxDepth) {
        this.mode = mode;
        this.block = block;
        this.maxDepth = maxDepth;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return TYPE;
    }

    @Override
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset, BlockPos pos, List<StructureBlockInfo> originals,
                                                       List<StructureBlockInfo> processed, StructurePlaceSettings settings) {
        BoundingBox area = settings.getBoundingBox();
        if (area == null || processed.isEmpty()) return processed;
        if (settings.getProcessors().stream().anyMatch(p -> p instanceof GravityProcessor)) return processed;

        int bottom = Integer.MAX_VALUE;
        for (StructureBlockInfo info : processed) {
            if (solid(info.state())) bottom = Math.min(bottom, info.pos().getY());
        }
        if (bottom == Integer.MAX_VALUE) return processed;

        // Lowest solid block per column; only columns that start in the bottom layer are grounded.
        Map<Long, StructureBlockInfo> lowest = new HashMap<>();
        for (StructureBlockInfo info : processed) {
            if (!solid(info.state())) continue;
            long column = column(info.pos());
            StructureBlockInfo current = lowest.get(column);
            if (current == null || info.pos().getY() < current.pos().getY()) lowest.put(column, info);
        }

        List<StructureBlockInfo> fill = new ArrayList<>();
        Set<Long> dropped = new HashSet<>();
        for (StructureBlockInfo base : lowest.values()) {
            BlockPos basePos = base.pos();
            if (basePos.getY() != bottom || !area.isInside(basePos)) continue;
            int depth = depthToGround(level, basePos.below(), area);
            if (depth == 0) continue;
            if (mode == Mode.EXTEND && depth > 0) {
                BlockState state = block.orElse(base.state());
                for (int d = 1; d <= depth; d++) fill.add(new StructureBlockInfo(basePos.below(d), state, null));
            } else {
                dropped.add(column(basePos));
            }
        }
        if (fill.isEmpty() && dropped.isEmpty()) return processed;

        List<StructureBlockInfo> result = new ArrayList<>(processed.size() + fill.size());
        for (StructureBlockInfo info : processed) {
            if (!dropped.contains(column(info.pos()))) result.add(info);
        }
        result.addAll(fill);
        return result;
    }

    /**
     * Air or fluid blocks between {@code start} and the ground: 0 if {@code start} is ground already,
     * -1 if the ground is deeper than {@code max_depth} or outside the area.
     */
    private int depthToGround(ServerLevelAccessor level, BlockPos start, BoundingBox area) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int depth = 0; depth <= maxDepth; depth++) {
            if (cursor.getY() < level.getMinBuildHeight() || !area.isInside(cursor)) return -1;
            if (ground(level.getBlockState(cursor))) return depth;
            cursor.move(0, -1, 0);
        }
        return -1;
    }

    private static boolean solid(BlockState state) {
        return !state.isAir() && state.blocksMotion();
    }

    private static boolean ground(BlockState state) {
        return state.blocksMotion() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    private static long column(BlockPos pos) {
        return ((long) pos.getX() << 32) | (pos.getZ() & 0xFFFFFFFFL);
    }
}
