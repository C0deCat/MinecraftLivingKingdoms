package livingkingdoms.world;

import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Guarantees a minimum land share for any seed (M1-C1). Players choose any seed and every world is
 * pre-generated at creation (D-20), so a seed whose map is nearly all ocean has to be corrected in the
 * generator, not reported. When the Overworld of a new save loads, before its first chunk, the land
 * share of the whole map is sampled from the generator noise; if it is below {@value #TARGET}, a lift
 * is found by bisection and added to the raw continentalness in {@link EdgeBias} (the same lever as
 * Tectonic's {@code ocean_offset}), so the continents grow while the edge still sinks to the open
 * ocean. The lift is stored in the save ({@code data/livingkingdoms_land.dat}), so a loaded world keeps
 * its terrain. A share at or above the target leaves the generator untouched (lift 0).
 */
final class LandLift {
    static final double TARGET = 0.25;
    static final double MAX_LIFT = 0.6;
    private static final int GRID = 128;
    private static final int STEPS = 9;
    private static final String NAME = "livingkingdoms_land";

    private static volatile double lift = 0;

    private LandLift() {}

    /** Added to the raw continentalness; read by worldgen threads. */
    static double current() {
        return lift;
    }

    static final class Data extends SavedData {
        private final double value;
        private final double before;
        private final double after;

        Data(double value, double before, double after) {
            this.value = value;
            this.before = before;
            this.after = after;
        }

        double value() {
            return value;
        }

        double before() {
            return before;
        }

        double after() {
            return after;
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            return new Data(tag.getDouble("lift"), tag.getDouble("land_before"), tag.getDouble("land_after"));
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putDouble("lift", value);
            tag.putDouble("land_before", before);
            tag.putDouble("land_after", after);
            return tag;
        }
    }

    static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        lift = 0;
        Data stored = level.getDataStorage().get(new SavedData.Factory<>(() -> new Data(0, 0, 0), Data::load), NAME);
        if (stored != null) {
            lift = stored.value;
            LivingKingdomsWorld.LOGGER.info("Land lift of this save: {}", stored.value);
            return;
        }
        long started = System.currentTimeMillis();
        WorldSize size = WorldSizeState.current();
        double before = landShare(level, size);
        double found = 0;
        double after = before;
        if (before < TARGET) {
            double low = 0;
            double high = MAX_LIFT;
            for (int i = 0; i < STEPS; i++) {
                double mid = (low + high) / 2;
                lift = mid;
                if (landShare(level, size) >= TARGET) high = mid;
                else low = mid;
            }
            found = high;
            lift = found;
            after = landShare(level, size);
        }
        lift = found;
        Data data = new Data(found, before, after);
        data.setDirty();
        level.getDataStorage().set(NAME, data);
        LivingKingdomsWorld.LOGGER.info(String.format(Locale.ROOT, "Land of this save (%s): %.1f%% of the map from the noise, lift %.4f -> %.1f%%, %d ms",
                size.id(), before * 100, found, after * 100, System.currentTimeMillis() - started));
    }

    /** Share of a {@value #GRID}² grid over the whole map with ground at or above sea level. */
    private static double landShare(ServerLevel level, WorldSize size) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();
        double step = size.side() / (double) GRID;
        double half = size.side() / 2.0;
        // Noise sampling is thread-safe (vanilla samples it from worker threads).
        long land = IntStream.range(0, GRID * GRID).parallel().filter(i -> {
            int x = (int) (-half + (i % GRID + 0.5) * step);
            int z = (int) (-half + (i / GRID + 0.5) * step);
            return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) >= seaLevel;
        }).count();
        return land / (double) (GRID * GRID);
    }
}
