package livingkingdoms.world;

import java.util.stream.IntStream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Ground height of the Overworld from the generator noise ({@code OCEAN_FLOOR_WG}: the top solid
 * block, the sea floor under water) on a {@value #STEP}-block grid over the whole map: 128² points on
 * Small, 192² on Medium, 256² on Large. Like the land lift and the {@code fixed_count} points it is a
 * fact of the save: computed once, when the Overworld of a new save loads (by {@link LandLift}, after
 * the lift is known), and stored in {@code data/livingkingdoms_heights.dat}. The validator reads it
 * for land share, settlement cells and the biome histogram instead of sampling the noise again.
 *
 * <p>Point {@code (i, j)} lies at the center of the {@value #STEP}-block square {@code i} columns east
 * and {@code j} rows south of the map's north-west corner, so every 128-block simulation cell holds
 * exactly 2 × 2 points. Noise heights, not placed blocks: after the pre-generation of D-20 the chunks'
 * own heightmaps are the truth.
 */
final class HeightGrid extends SavedData {
    static final int STEP = 64;
    static final String NAME = "livingkingdoms_heights";

    private final int side;
    private final int[] heights;

    private HeightGrid(int side, int[] heights) {
        this.side = side;
        this.heights = heights;
    }

    /** Points per row and column. */
    int points() {
        return side / STEP;
    }

    int x(int i) {
        return -side / 2 + STEP / 2 + i * STEP;
    }

    int z(int j) {
        return x(j);
    }

    int height(int i, int j) {
        return heights[j * points() + i];
    }

    /** Share of the points with ground at or above sea level. */
    double landShare(int seaLevel) {
        long land = IntStream.of(heights).filter(h -> h >= seaLevel).count();
        return land / (double) heights.length;
    }

    /** Samples the noise of the level's generator now, in parallel (noise sampling is thread-safe). */
    static HeightGrid compute(ServerLevel level, WorldSize size) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        HeightGrid grid = new HeightGrid(size.side(), new int[(size.side() / STEP) * (size.side() / STEP)]);
        int n = grid.points();
        IntStream.range(0, n * n).parallel().forEach(k -> grid.heights[k] =
                generator.getBaseHeight(grid.x(k % n), grid.z(k / n), Heightmap.Types.OCEAN_FLOOR_WG, level, random));
        return grid;
    }

    /** The grid stored in this save, or null (a save created before the grid existed). */
    static HeightGrid stored(ServerLevel overworld) {
        HeightGrid grid = overworld.getDataStorage().get(new SavedData.Factory<>(() -> new HeightGrid(0, new int[0]), HeightGrid::load), NAME);
        return grid != null && grid.side > 0 ? grid : null;
    }

    /** Stores the grid in the save. Server thread. */
    void store(ServerLevel overworld) {
        setDirty();
        overworld.getDataStorage().set(NAME, this);
    }

    static HeightGrid load(CompoundTag tag, HolderLookup.Provider registries) {
        // A grid of another step (an older format) is ignored and computed again.
        if (tag.getInt("step") != STEP) return new HeightGrid(0, new int[0]);
        return new HeightGrid(tag.getInt("side"), tag.getIntArray("heights"));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("step", STEP);
        tag.putInt("side", side);
        tag.putIntArray("heights", heights);
        return tag;
    }
}
