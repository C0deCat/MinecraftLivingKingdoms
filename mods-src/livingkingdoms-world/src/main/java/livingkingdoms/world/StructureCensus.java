package livingkingdoms.world;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /lkworld structures [radius]}: a census of structure starts in the caller's dimension,
 * in a square of the given radius (blocks) around the origin. For every valid start it records
 * the structure, its bounding box and whether the box is inside the world border. Surface
 * structures in dimensions without a ceiling also get the terrain under their pieces, sampled
 * from the generator noise (so the structure's own blocks do not count): the worst piece slope
 * (relief under one piece divided by its longer side), that piece's relief, and the share of the
 * sampled ground below sea level. Only pieces at least {@value #MIN_PIECE} blocks long whose top
 * reaches the lowest ground under them count, so buried rooms, shafts and path tiles do not; a
 * start without such pieces is not measured. Pairs of different starts whose pieces intersect are listed as
 * overlaps; the overall boxes of jigsaw structures include empty space and only find candidates.
 *
 * <p>Chunks are read at {@code structure_starts} status, so the area does not need to be fully
 * generated; pre-generating it with Chunky makes the run faster. Output:
 * {@code <world>/livingkingdoms/structures-<dimension>-<size>.csv} and
 * {@code overlaps-<dimension>-<size>.csv}, and a summary logged as {@code Structures <dimension> <size>: ...}.
 */
final class StructureCensus {
    static final double STEEP_SLOPE = 0.5;
    static final double WET_SHARE = 0.5;
    private static final int SAMPLE_STEP = 4;
    private static final int MIN_PIECE = 8;
    private static final int TOP = 12;

    private StructureCensus() {}

    record Start(String id, String step, BoundingBox box, List<BoundingBox> pieces, boolean inside, double slope, int relief, double water) {
        String namespace() {
            return id.substring(0, id.indexOf(':'));
        }

        boolean surface() {
            return step.equals(GenerationStep.Decoration.SURFACE_STRUCTURES.getName());
        }

        boolean flagged() {
            return slope > STEEP_SLOPE || water > WET_SHARE;
        }
    }

    record Overlap(Start a, Start b, long volume) {}

    static int start(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        WorldSize size = WorldSizeState.current();
        source.sendSuccess(() -> Component.literal("Counting structures in " + level.dimension().location()
                + " within " + radius + " blocks in the background..."), false);
        Thread thread = new Thread(() -> {
            try {
                run(source, level, size, radius);
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("Structure census failed", e);
            }
        }, "livingkingdoms-structures");
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    private static void run(CommandSourceStack source, ServerLevel level, WorldSize size, int radius) {
        long started = System.currentTimeMillis();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        WorldBorder border = level.getWorldBorder();
        // Terrain is only meaningful in natural dimensions without a ceiling (not the Nether, not the End void).
        boolean terrain = level.dimensionType().natural() && !level.dimensionType().hasCeiling();
        int seaLevel = generator.getSeaLevel();

        int chunkRadius = Math.floorDiv(radius, 16);
        List<Start> starts = new ArrayList<>();
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                // Off the server thread this call hands the load to the server thread and waits for it.
                ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS, true);
                if (chunk == null) continue;
                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    StructureStart start = entry.getValue();
                    if (!start.isValid()) continue;
                    ResourceLocation key = registry.getKey(entry.getKey());
                    String id = key != null ? key.toString() : "unknown:unregistered";
                    String step = entry.getKey().step().getName();
                    BoundingBox box = start.getBoundingBox();
                    List<BoundingBox> pieces = start.getPieces().stream().map(piece -> piece.getBoundingBox()).toList();
                    boolean inside = border.isWithinBounds(box.minX(), box.minZ()) && border.isWithinBounds(box.maxX(), box.maxZ());
                    double slope = -1;
                    int relief = -1;
                    double water = -1;
                    if (terrain && step.equals(GenerationStep.Decoration.SURFACE_STRUCTURES.getName())) {
                        int wet = 0;
                        int total = 0;
                        for (BoundingBox piece : pieces) {
                            // Small pieces (paths, shafts) exaggerate slope; buried pieces do not show the terrain.
                            if (Math.max(piece.getXSpan(), piece.getZSpan()) < MIN_PIECE) continue;
                            int[] ground = footprint(generator, level, random, piece, seaLevel);
                            if (piece.maxY() < ground[3]) continue;
                            double pieceSlope = ground[0] / (double) Math.max(1, Math.max(piece.getXSpan(), piece.getZSpan()));
                            if (pieceSlope > slope) {
                                slope = pieceSlope;
                                relief = ground[0];
                            }
                            wet += ground[1];
                            total += ground[2];
                        }
                        water = total > 0 ? wet / (double) total : 0;
                    }
                    starts.add(new Start(id, step, box, pieces, inside, slope, relief, water));
                }
            }
        }
        List<Overlap> overlaps = overlaps(starts);
        String dimension = level.dimension().location().getPath();
        Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("livingkingdoms");
        Path startsFile = dir.resolve("structures-" + dimension + "-" + size.id() + ".csv");
        Path overlapsFile = dir.resolve("overlaps-" + dimension + "-" + size.id() + ".csv");
        try {
            Files.createDirectories(dir);
            writeStarts(startsFile, starts);
            writeOverlaps(overlapsFile, overlaps);
        } catch (IOException e) {
            LivingKingdomsWorld.LOGGER.error("Could not write the structure census to {}", dir, e);
            return;
        }
        String message = summary(level, size, radius, starts, overlaps, System.currentTimeMillis() - started, startsFile);
        for (String line : message.split("\n")) LivingKingdomsWorld.LOGGER.info(line);
        String first = message.substring(0, message.indexOf('\n') < 0 ? message.length() : message.indexOf('\n'));
        source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(first), false));
    }

    /** Relief, wet samples, total samples and lowest ground under a box, from the generator noise. */
    private static int[] footprint(ChunkGenerator generator, ServerLevel level, RandomState random, BoundingBox box, int seaLevel) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int wet = 0;
        int total = 0;
        for (int x = box.minX(); x <= box.maxX(); x += SAMPLE_STEP) {
            for (int z = box.minZ(); z <= box.maxZ(); z += SAMPLE_STEP) {
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                min = Math.min(min, height);
                max = Math.max(max, height);
                if (height < seaLevel) wet++;
                total++;
            }
        }
        return new int[] {max - min, wet, total, min};
    }

    /** Intersecting pairs of starts, found with a sweep over minX. */
    private static List<Overlap> overlaps(List<Start> starts) {
        List<Start> sorted = new ArrayList<>(starts);
        sorted.sort(Comparator.comparingInt(s -> s.box().minX()));
        List<Overlap> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Start a = sorted.get(i);
            for (int j = i + 1; j < sorted.size(); j++) {
                Start b = sorted.get(j);
                if (b.box().minX() > a.box().maxX()) break;
                if (!a.box().intersects(b.box())) continue;
                long volume = 0;
                for (BoundingBox pa : a.pieces()) {
                    for (BoundingBox pb : b.pieces()) {
                        if (pa.intersects(pb)) volume += intersection(pa, pb);
                    }
                }
                if (volume > 0) result.add(new Overlap(a, b, volume));
            }
        }
        return result;
    }

    private static long intersection(BoundingBox a, BoundingBox b) {
        long dx = Math.min(a.maxX(), b.maxX()) - Math.max(a.minX(), b.minX()) + 1L;
        long dy = Math.min(a.maxY(), b.maxY()) - Math.max(a.minY(), b.minY()) + 1L;
        long dz = Math.min(a.maxZ(), b.maxZ()) - Math.max(a.minZ(), b.minZ()) + 1L;
        return dx * dy * dz;
    }

    private static String summary(ServerLevel level, WorldSize size, int radius, List<Start> starts, List<Overlap> overlaps,
                                  long millis, Path file) {
        StringBuilder out = new StringBuilder();
        long surface = starts.stream().filter(Start::surface).count();
        long outside = starts.stream().filter(s -> !s.inside()).count();
        List<Start> measured = starts.stream().filter(s -> s.slope() >= 0).toList();
        long steep = measured.stream().filter(s -> s.slope() > STEEP_SLOPE).count();
        long wet = measured.stream().filter(s -> s.water() > WET_SHARE).count();
        List<Overlap> surfacePairs = overlaps.stream().filter(o -> o.a().surface() && o.b().surface()).toList();
        out.append(String.format(Locale.ROOT,
                "Structures %s %s: radius %d, %d starts (%d surface, %d outside the border), %d overlapping pairs (%d surface-surface), "
                        + "%d of %d measured surface starts with a piece slope > %.1f, %d with > %.0f%% water, %d ms -> %s",
                level.dimension().location(), size.id(), radius, starts.size(), surface, outside, overlaps.size(),
                surfacePairs.size(), steep, measured.size(), STEEP_SLOPE, wet, WET_SHARE * 100, millis, file.toAbsolutePath()));
        Map<String, Long> perNamespace = new TreeMap<>(starts.stream().collect(Collectors.groupingBy(Start::namespace, Collectors.counting())));
        out.append("\n  per mod: ").append(perNamespace);
        out.append("\n  overlapping pairs (surface-surface first):");
        Map<String, Long> pairs = new HashMap<>();
        for (Overlap o : overlaps) {
            String a = o.a().id();
            String b = o.b().id();
            String key = (o.a().surface() && o.b().surface() ? "S " : "  ") + (a.compareTo(b) <= 0 ? a + " x " + b : b + " x " + a);
            pairs.merge(key, 1L, Long::sum);
        }
        pairs.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByKey(Comparator.comparing(k -> !k.startsWith("S")))
                        .thenComparing(Map.Entry.<String, Long>comparingByValue().reversed()))
                .limit(TOP)
                .forEach(e -> out.append("\n    ").append(e.getValue()).append(" ").append(e.getKey()));
        out.append("\n  steep or wet surface structures (flagged/all, mean worst-piece slope, mean water):");
        measured.stream()
                .collect(Collectors.groupingBy(Start::id))
                .entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .filter(e -> e.getValue().stream().anyMatch(Start::flagged))
                .sorted(Comparator.comparingLong(e -> -e.getValue().stream().filter(Start::flagged).count()))
                .limit(TOP)
                .forEach(e -> {
                    List<Start> list = e.getValue();
                    long bad = list.stream().filter(Start::flagged).count();
                    double slope = list.stream().mapToDouble(Start::slope).average().orElse(0);
                    double water = list.stream().mapToDouble(Start::water).average().orElse(0);
                    out.append(String.format(Locale.ROOT, "\n    %d/%d %s (slope %.2f, water %.0f%%)", bad, list.size(), e.getKey(), slope, water * 100));
                });
        return out.toString();
    }

    private static void writeStarts(Path file, List<Start> starts) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("id,namespace,step,min_x,min_y,min_z,max_x,max_y,max_z,pieces,inside_border,piece_slope,piece_relief,water_share\n");
            for (Start s : starts) {
                BoundingBox b = s.box();
                w.write(String.format(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%b,%.3f,%d,%.3f%n", s.id(), s.namespace(), s.step(),
                        b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), s.pieces().size(), s.inside(), s.slope(), s.relief(), s.water()));
            }
        }
    }

    private static void writeOverlaps(Path file, List<Overlap> overlaps) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("a,b,a_center_x,a_center_z,b_center_x,b_center_z,volume,both_surface\n");
            for (Overlap o : overlaps) {
                BoundingBox a = o.a().box();
                BoundingBox b = o.b().box();
                w.write(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%d,%b%n", o.a().id(), o.b().id(),
                        a.getCenter().getX(), a.getCenter().getZ(), b.getCenter().getX(), b.getCenter().getZ(), o.volume(),
                        o.a().surface() && o.b().surface()));
            }
        }
    }
}
