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
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import livingkingdoms.world.StructureScan.Area;
import livingkingdoms.world.StructureScan.Overlap;
import livingkingdoms.world.StructureScan.Result;
import livingkingdoms.world.StructureScan.Start;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /lkworld structures [radius | border | all]}: a census of structure starts. With a radius,
 * a square around the origin in the caller's dimension; {@code border}, the whole inside of the
 * caller's dimension border; {@code all}, the whole inside of every dimension's border. The starts
 * and their measurements come from {@link StructureScan}; pairs of different starts whose pieces
 * intersect are listed as overlaps.
 *
 * <p>Output per dimension: {@code <world>/livingkingdoms/structures-<dimension>-<size>.csv} and
 * {@code overlaps-<dimension>-<size>.csv}, and a summary logged as {@code Structures <dimension> <size>: ...}.
 * {@code all} ends with the line {@code Structures done: ...}.
 */
final class StructureCensus {
    static final double STEEP_SLOPE = 0.5;
    static final double WET_SHARE = 0.5;
    static final double HANGING_SHARE = 0.25;
    private static final int TOP = 12;

    private StructureCensus() {}

    /** Square of the radius in the caller's dimension. */
    static int start(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        return startIn(source, List.of(level), lvl -> Area.square(radius));
    }

    /** The whole inside of the border, in the caller's dimension or in every dimension. */
    static int startBorder(CommandSourceStack source, boolean allLevels) {
        List<ServerLevel> levels = new ArrayList<>();
        if (allLevels) source.getServer().getAllLevels().forEach(levels::add);
        else levels.add(source.getLevel());
        return startIn(source, levels, Area::border);
    }

    private static int startIn(CommandSourceStack source, List<ServerLevel> levels, Function<ServerLevel, Area> areas) {
        WorldSize size = WorldSizeState.current();
        source.sendSuccess(() -> Component.literal("Counting structures in " + levels.size() + " dimension(s) in the background..."), false);
        Thread thread = new Thread(() -> {
            long started = System.currentTimeMillis();
            int total = 0;
            try {
                for (ServerLevel level : levels) total += run(source, level, size, areas.apply(level));
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("Structure census failed", e);
                return;
            }
            if (levels.size() > 1) {
                String done = String.format(Locale.ROOT, "Structures done: %d dimensions, %d starts, %d ms", levels.size(), total,
                        System.currentTimeMillis() - started);
                LivingKingdomsWorld.LOGGER.info(done);
                source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(done), false));
            }
        }, "livingkingdoms-structures");
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    private static int run(CommandSourceStack source, ServerLevel level, WorldSize size, Area area) {
        Result result = StructureScan.scan(level, area);
        List<Start> starts = result.starts();
        List<Overlap> overlaps = StructureScan.overlaps(starts);
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
            return starts.size();
        }
        String message = summary(level, size, area, result, overlaps, startsFile);
        for (String line : message.split("\n")) LivingKingdomsWorld.LOGGER.info(line);
        String first = message.substring(0, message.indexOf('\n') < 0 ? message.length() : message.indexOf('\n'));
        source.getServer().execute(() -> source.sendSuccess(() -> Component.literal(first), false));
        return starts.size();
    }

    private static String summary(ServerLevel level, WorldSize size, Area area, Result result, List<Overlap> overlaps, Path file) {
        List<Start> starts = result.starts();
        StringBuilder out = new StringBuilder();
        long surface = starts.stream().filter(Start::surface).count();
        long outside = starts.stream().filter(s -> !s.inside()).count();
        List<Start> measured = starts.stream().filter(Start::measured).toList();
        long steep = measured.stream().filter(s -> s.slope() > STEEP_SLOPE).count();
        long buried = measured.stream().filter(s -> s.entrance().equals("buried")).count();
        long floating = measured.stream().filter(s -> s.entrance().equals("floating")).count();
        long wet = measured.stream().filter(s -> s.water() > WET_SHARE).count();
        long hanging = measured.stream().filter(s -> s.hanging() > HANGING_SHARE).count();
        List<Overlap> surfacePairs = overlaps.stream().filter(o -> o.a().surface() && o.b().surface()).toList();
        out.append(String.format(Locale.ROOT,
                "Structures %s %s: %s, %d starts (%d surface, %d outside the border), %d overlapping pairs (%d surface-surface), "
                        + "%d of %d measured surface starts with a piece slope > %.1f, %d with > %.0f%% water, %d with a piece > %.0f%% hanging, "
                        + "entrances %d buried / %d floating, %d of %d chunks loaded (%s), %d ms -> %s",
                level.dimension().location(), size.id(), area.label(), starts.size(), surface, outside, overlaps.size(),
                surfacePairs.size(), steep, measured.size(), STEEP_SLOPE, wet, WET_SHARE * 100, hanging, HANGING_SHARE * 100,
                buried, floating, result.chunksLoaded(), result.chunksInArea(), result.method(), result.millis(), file.toAbsolutePath()));
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
        out.append("\n  buried or floating entrances (buried/floating/measured):");
        measured.stream()
                .collect(Collectors.groupingBy(Start::id))
                .entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(s -> !s.entrance().equals("ok")))
                .sorted(Comparator.comparingLong(e -> -e.getValue().stream().filter(s -> !s.entrance().equals("ok")).count()))
                .limit(TOP)
                .forEach(e -> out.append(String.format(Locale.ROOT, "\n    %d/%d/%d %s",
                        e.getValue().stream().filter(s -> s.entrance().equals("buried")).count(),
                        e.getValue().stream().filter(s -> s.entrance().equals("floating")).count(),
                        e.getValue().size(), e.getKey())));
        out.append("\n  hanging pieces (starts with a piece > 25% hanging/measured, mean worst share, terrain adaptation):");
        measured.stream()
                .collect(Collectors.groupingBy(Start::id))
                .entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(s -> s.hanging() > HANGING_SHARE))
                .sorted(Comparator.comparingLong(e -> -e.getValue().stream().filter(s -> s.hanging() > HANGING_SHARE).count()))
                .limit(TOP)
                .forEach(e -> {
                    List<Start> list = e.getValue();
                    long bad = list.stream().filter(s -> s.hanging() > HANGING_SHARE).count();
                    double mean = list.stream().mapToDouble(Start::hanging).filter(v -> v >= 0).average().orElse(0);
                    out.append(String.format(Locale.ROOT, "\n    %d/%d %s (%.0f%%, %s)", bad, list.size(), e.getKey(), mean * 100,
                            list.get(0).adaptation()));
                });
        out.append("\n  steep or wet surface structures (flagged/all, mean worst-piece slope, mean water):");
        measured.stream()
                .collect(Collectors.groupingBy(Start::id))
                .entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(s -> s.slope() > STEEP_SLOPE || s.water() > WET_SHARE))
                .sorted(Comparator.comparingLong(e -> -e.getValue().stream().filter(s -> s.slope() > STEEP_SLOPE || s.water() > WET_SHARE).count()))
                .limit(TOP)
                .forEach(e -> {
                    List<Start> list = e.getValue();
                    long bad = list.stream().filter(s -> s.slope() > STEEP_SLOPE || s.water() > WET_SHARE).count();
                    double slope = list.stream().mapToDouble(Start::slope).filter(v -> v >= 0).average().orElse(0);
                    double water = list.stream().mapToDouble(Start::water).average().orElse(0);
                    out.append(String.format(Locale.ROOT, "\n    %d/%d %s (slope %.2f, water %.0f%%)", bad, list.size(), e.getKey(), slope, water * 100));
                });
        return out.toString();
    }

    private static void writeStarts(Path file, List<Start> starts) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("id,namespace,step,min_x,min_y,min_z,max_x,max_y,max_z,pieces,inside_border,piece_slope,piece_relief,water_share,"
                    + "entrance,entrance_x,entrance_y,entrance_z,adaptation,biome,hanging_share,hanging_x,hanging_y,hanging_z,hanging_template,template_namespaces\n");
            for (Start s : starts) {
                BoundingBox b = s.box();
                BoundingBox e = s.entranceBox();
                BoundingBox h = s.hangingPiece();
                String namespaces = s.templates().stream().map(t -> t.substring(0, t.indexOf(':')))
                        .collect(Collectors.toCollection(TreeSet::new)).stream().collect(Collectors.joining("|"));
                w.write(String.format(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%b,%.3f,%d,%.3f,%s,%d,%d,%d,%s,%s,%.3f,%d,%d,%d,%s,%s%n",
                        s.id(), s.namespace(), s.step(), b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ(), s.pieces().size(),
                        s.inside(), s.slope(), s.relief(), s.water(), s.entrance(), e.getCenter().getX(), e.maxY(), e.getCenter().getZ(),
                        s.adaptation(), s.biome(), s.hanging(), h.getCenter().getX(), h.minY(), h.getCenter().getZ(), s.hangingTemplate(), namespaces));
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
