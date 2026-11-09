package livingkingdoms.world;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import livingkingdoms.world.ContentManifest.Access;
import livingkingdoms.world.ContentManifest.Entry;
import livingkingdoms.world.ContentManifest.Severity;
import livingkingdoms.world.StructureScan.Area;
import livingkingdoms.world.StructureScan.Overlap;
import livingkingdoms.world.StructureScan.Start;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /lkworld validate}: checks the world against the {@link ContentManifest} (M1-C1). For every
 * dimension the manifest names it scans all structure starts inside the border ({@link StructureScan},
 * from the generator, no chunks generated) and, per entry, counts the starts that are inside the border
 * and pass the entry's access rule, against the entry's minimum for the world size. Also checked: eyes
 * of ender lead to a stronghold, no Dungeons and Taverns pieces in Nether fortresses, overlaps that
 * touch mandatory structures, the land share, witch huts in swamps and free land for settlements.
 *
 * <p>Output: {@code <world>/livingkingdoms/validation-<size>.json} and {@code .md}, and the log line
 * {@code Validation <size> <seed>: N critical, N warnings -> <path>}. Test is a development size
 * (ADR-0004): its counts are reported, not judged.
 */
final class WorldValidator {
    private static final int EYE_TOLERANCE = 96;
    private static final int CELL = 128;
    private static final int CELL_RELIEF = 24;

    private WorldValidator() {}

    record Issue(Severity severity, String entry, String message) {}

    record EntryResult(Entry entry, int found, int reachable, int kinds, Integer min, List<String> failures) {}

    static int start(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Component.literal("Validating the world against the content manifest in the background..."), false);
        Thread thread = new Thread(() -> {
            try {
                run(source, server);
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("World validation failed", e);
                // The summary line still appears, so scripts waiting for it do not hang.
                LivingKingdomsWorld.LOGGER.info("Validation {} {}: failed with {}", WorldSizeState.current().id(), server.overworld().getSeed(), e);
            }
        }, "livingkingdoms-validate");
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    private static void run(CommandSourceStack source, MinecraftServer server) {
        long started = System.currentTimeMillis();
        WorldSize size = WorldSizeState.current();
        ServerLevel overworld = server.overworld();
        Registry<Structure> registry = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Entry> entries = ContentManifest.entries();
        List<Issue> issues = new ArrayList<>();
        List<String> info = new ArrayList<>();
        if (entries.isEmpty()) issues.add(new Issue(Severity.CRITICAL, "manifest", "the content manifest is empty"));
        if (size == WorldSize.TEST) info.add("Test is a development size (ADR-0004): counts are reported, not judged.");

        // Structure ids per entry; tags are expanded.
        Map<Entry, Set<String>> ids = new LinkedHashMap<>();
        for (Entry entry : entries) {
            Set<String> set = new LinkedHashSet<>();
            for (String ref : entry.structures()) {
                if (ref.startsWith("#")) {
                    TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, ResourceLocation.parse(ref.substring(1)));
                    registry.getTag(tag).ifPresentOrElse(
                            holders -> holders.forEach(h -> h.unwrapKey().ifPresent(k -> set.add(k.location().toString()))),
                            () -> issues.add(new Issue(Severity.WARNING, entry.id().toString(), "unknown structure tag " + ref)));
                } else if (registry.containsKey(ResourceLocation.parse(ref))) {
                    set.add(ref);
                } else {
                    issues.add(new Issue(Severity.WARNING, entry.id().toString(), "unknown structure " + ref));
                }
            }
            ids.put(entry, set);
        }

        // Scan every dimension the manifest names, plus the Overworld for the other checks.
        Set<ResourceLocation> dimensions = new LinkedHashSet<>();
        dimensions.add(Level.OVERWORLD.location());
        entries.forEach(e -> dimensions.add(e.dimension()));
        Map<ResourceLocation, List<Start>> starts = new LinkedHashMap<>();
        Map<ResourceLocation, List<Overlap>> overlaps = new HashMap<>();
        for (ResourceLocation dimension : dimensions) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level == null) {
                issues.add(new Issue(Severity.CRITICAL, "dimension", "dimension " + dimension + " does not exist"));
                continue;
            }
            Set<String> measured = new HashSet<>();
            for (Entry entry : entries) {
                if (!entry.dimension().equals(dimension)) continue;
                for (String id : ids.get(entry)) {
                    Access access = entry.accessFor(id);
                    if (!access.entrance().equals("any") || access.maxHanging() >= 0 || !access.allowWater()) measured.add(id);
                }
            }
            List<Start> found = StructureScan.scan(level, Area.border(level), measured::contains).starts();
            starts.put(dimension, found);
            overlaps.put(dimension, StructureScan.overlaps(found));
            LivingKingdomsWorld.LOGGER.info("Validation: scanned {}, {} starts", dimension, found.size());
        }

        // Manifest entries.
        List<EntryResult> results = new ArrayList<>();
        Set<String> mandatory = new HashSet<>();
        for (Entry entry : entries) {
            List<Start> dimensionStarts = starts.getOrDefault(entry.dimension(), List.of());
            Set<String> wanted = ids.get(entry);
            if (entry.severity() == Severity.CRITICAL) mandatory.addAll(wanted);
            List<Start> matching = dimensionStarts.stream().filter(s -> wanted.contains(s.id()) && s.inside()).toList();
            List<String> failures = new ArrayList<>();
            List<Start> reachable = new ArrayList<>();
            for (Start s : matching) {
                String failure = accessFailure(s, entry.accessFor(s.id()));
                if (failure == null) reachable.add(s);
                else failures.add(s.id() + " at " + at(s) + ": " + failure);
            }
            int kinds = (int) reachable.stream().map(Start::id).distinct().count();
            Integer min = entry.min().get(size);
            Integer max = entry.max().get(size);
            String name = entry.id().toString();
            if (size != WorldSize.TEST) {
                if (min != null && reachable.size() < min) {
                    issues.add(new Issue(entry.severity(), name, String.format(Locale.ROOT, "%d reachable of at least %d (%d inside the border)",
                            reachable.size(), min, matching.size())));
                }
                if (max != null && matching.size() > max) {
                    issues.add(new Issue(Severity.WARNING, name, String.format(Locale.ROOT, "%d inside the border, more than %d", matching.size(), max)));
                }
                if (entry.minKinds() > 0 && kinds < entry.minKinds()) {
                    issues.add(new Issue(entry.severity(), name, String.format(Locale.ROOT, "%d kinds reachable of at least %d", kinds, entry.minKinds())));
                }
            }
            for (String failure : failures) issues.add(new Issue(Severity.WARNING, name, "not reachable: " + failure));
            if (entry.spacing() > 0) {
                for (int i = 0; i < reachable.size(); i++) {
                    for (int j = i + 1; j < reachable.size(); j++) {
                        double d = distance(reachable.get(i), reachable.get(j));
                        if (d < entry.spacing()) {
                            issues.add(new Issue(Severity.WARNING, name, String.format(Locale.ROOT, "%s at %s and %s at %s are %.0f blocks apart (spacing %d)",
                                    reachable.get(i).id(), at(reachable.get(i)), reachable.get(j).id(), at(reachable.get(j)), d, entry.spacing())));
                        }
                    }
                }
            }
            results.add(new EntryResult(entry, matching.size(), reachable.size(), kinds, min, failures));
        }

        // Overlaps that touch a mandatory structure.
        overlaps.forEach((dimension, list) -> {
            for (Overlap o : list) {
                // Pairs of one mod are its own layout (the Twilight Forest places all landmarks in one grid).
                if (o.a().namespace().equals(o.b().namespace())) continue;
                if (mandatory.contains(o.a().id()) || mandatory.contains(o.b().id())) {
                    issues.add(new Issue(Severity.WARNING, "overlaps", String.format(Locale.ROOT, "%s at %s cuts into %s at %s (%d blocks) in %s",
                            o.a().id(), at(o.a()), o.b().id(), at(o.b()), o.volume(), dimension)));
                }
            }
        });

        checkEyesOfEnder(server, overworld, size, starts.getOrDefault(Level.OVERWORLD.location(), List.of()), issues, info);
        checkFortresses(starts.getOrDefault(Level.NETHER.location(), List.of()), issues, info);
        checkWitchHuts(starts.getOrDefault(Level.OVERWORLD.location(), List.of()), info);
        checkLand(overworld, issues, info);
        checkSettlementLand(overworld, size, starts.getOrDefault(Level.OVERWORLD.location(), List.of()), info);
        surfaceBiomes(overworld, size, info);

        long critical = issues.stream().filter(i -> i.severity() == Severity.CRITICAL).count();
        long warnings = issues.size() - critical;
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("livingkingdoms");
        Path json = dir.resolve("validation-" + size.id() + ".json");
        Path markdown = dir.resolve("validation-" + size.id() + ".md");
        long seed = overworld.getSeed();
        try {
            Files.createDirectories(dir);
            Files.writeString(json, toJson(size, seed, results, issues, info), StandardCharsets.UTF_8);
            Files.writeString(markdown, toMarkdown(size, seed, results, issues, info), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LivingKingdomsWorld.LOGGER.error("Could not write the validation report to {}", dir, e);
        }
        String line = String.format(Locale.ROOT, "Validation %s %d: %d critical, %d warnings, %d entries, %d ms -> %s", size.id(), seed, critical,
                warnings, entries.size(), System.currentTimeMillis() - started, markdown.toAbsolutePath());
        for (Issue issue : issues) {
            if (issue.severity() == Severity.CRITICAL) LivingKingdomsWorld.LOGGER.warn("Validation critical: {}: {}", issue.entry(), issue.message());
        }
        LivingKingdomsWorld.LOGGER.info(line);
        server.execute(() -> source.sendSuccess(() -> Component.literal(line), false));
    }

    /** Why a start is not reachable under the access rule, or null. Unmeasured starts pass. */
    private static String accessFailure(Start s, Access access) {
        if (!s.measured()) return null;
        if (access.entrance().equals("ok") && !s.entrance().equals("ok")) return "entrance " + s.entrance();
        if (access.entrance().equals("buried_ok") && s.entrance().equals("floating")) return "entrance floating";
        if (access.maxHanging() >= 0 && s.hanging() > access.maxHanging()) {
            return String.format(Locale.ROOT, "a piece %.0f%% hanging at %s", s.hanging() * 100, center(s.hangingPiece()));
        }
        if (!access.allowWater() && s.water() > StructureCensus.WET_SHARE) return String.format(Locale.ROOT, "%.0f%% under water", s.water() * 100);
        return null;
    }

    /** TD-16: eyes of ender thrown at several land points lead to a stronghold start. */
    private static void checkEyesOfEnder(MinecraftServer server, ServerLevel level, WorldSize size, List<Start> starts, List<Issue> issues,
                                         List<String> info) {
        List<Start> strongholds = starts.stream().filter(s -> s.id().equals("betterstrongholds:stronghold") || s.id().equals("minecraft:stronghold")).toList();
        int quarter = size.side() / 4;
        int[][] points = {{0, 0}, {quarter, quarter}, {-quarter, quarter}, {quarter, -quarter}, {-quarter, -quarter}};
        int good = 0;
        for (int[] p : points) {
            BlockPos from = new BlockPos(p[0], 64, p[1]);
            BlockPos target = server.submit(() -> level.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, from, 100, false)).join();
            if (target == null) {
                issues.add(new Issue(Severity.CRITICAL, "eye_of_ender", "an eye of ender thrown at " + from.toShortString() + " finds nothing"));
                continue;
            }
            boolean atStronghold = strongholds.stream().anyMatch(s -> s.box().inflatedBy(EYE_TOLERANCE).isInside(target.getX(), s.box().minY(), target.getZ()));
            if (atStronghold) good++;
            else issues.add(new Issue(Severity.CRITICAL, "eye_of_ender", "an eye of ender thrown at " + from.toShortString() + " leads to "
                    + target.toShortString() + ", not to a stronghold"));
        }
        info.add(String.format(Locale.ROOT, "Eyes of ender: %d of %d throws lead to a stronghold (%d strongholds inside the border).", good,
                points.length, strongholds.stream().filter(Start::inside).count()));
    }

    /** TD-17: Dungeons and Taverns ships pools under minecraft:nether_fortress; YUNG's fortresses must not use them. */
    private static void checkFortresses(List<Start> nether, List<Issue> issues, List<String> info) {
        List<Start> fortresses = nether.stream().filter(s -> s.id().endsWith("fortress") && !s.id().startsWith("structory")).toList();
        long mixed = 0;
        for (Start s : fortresses) {
            if (s.templates().stream().anyMatch(t -> t.startsWith("nova_structures:"))) {
                mixed++;
                issues.add(new Issue(Severity.WARNING, "fortress", s.id() + " at " + at(s) + " contains Dungeons and Taverns pieces"));
            }
        }
        info.add(String.format(Locale.ROOT, "Nether fortresses: %d, %d with Dungeons and Taverns pieces.", fortresses.size(), mixed));
    }

    /** TD-18: witch huts of YUNG's, Terralith and Dungeons and Taverns in swamp biomes. */
    private static void checkWitchHuts(List<Start> overworld, List<String> info) {
        Map<String, Long> huts = new LinkedHashMap<>();
        long swamp = overworld.stream().filter(s -> s.biome().contains("swamp") && s.inside()).count();
        overworld.stream().filter(s -> s.id().contains("witch") && s.inside())
                .forEach(s -> huts.merge(s.id() + (s.biome().contains("swamp") ? " (swamp)" : ""), 1L, Long::sum));
        info.add("Witch huts: " + (huts.isEmpty() ? "none" : huts) + "; all starts in swamp biomes: " + swamp + ".");
    }

    private static void checkLand(ServerLevel overworld, List<Issue> issues, List<String> info) {
        var data = overworld.getDataStorage().get(new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                () -> new LandLift.Data(0, 0, 0), LandLift.Data::load), "livingkingdoms_land");
        if (data == null) {
            issues.add(new Issue(Severity.WARNING, "land", "no land calibration in this save (created before the land lift)"));
            return;
        }
        info.add(String.format(Locale.ROOT, "Land: %.1f%% of the map from the noise, lift %.4f, %.1f%% after.", data.before() * 100, data.value(),
                data.after() * 100));
        if (data.after() < LandLift.TARGET) {
            issues.add(new Issue(Severity.CRITICAL, "land", String.format(Locale.ROOT, "land share %.1f%% below %.0f%%", data.after() * 100,
                    LandLift.TARGET * 100)));
        }
    }

    /**
     * Free land for settlements (reported, not reserved): 128-block cells of the simulation grid inside
     * the open-ocean ring whose five samples are land, with relief of at most 24 blocks, and that no
     * surface structure touches. M4 and M6 plan 30-60 settlements and 6-10 states.
     */
    private static void checkSettlementLand(ServerLevel level, WorldSize size, List<Start> starts, List<String> info) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int seaLevel = generator.getSeaLevel();
        int cells = size.side() / CELL;
        int half = size.side() / 2;
        List<BoundingBox> boxes = starts.stream().filter(s -> s.surface() && s.inside()).map(Start::box).toList();
        long free = IntStream.range(0, cells * cells).parallel().filter(i -> {
            int x0 = -half + (i % cells) * CELL;
            int z0 = -half + (i / cells) * CELL;
            int[][] samples = {{64, 64}, {16, 16}, {112, 16}, {16, 112}, {112, 112}};
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int[] s : samples) {
                int h = generator.getBaseHeight(x0 + s[0], z0 + s[1], Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                if (h < seaLevel) return false;
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
            if (max - min > CELL_RELIEF) return false;
            BoundingBox cell = new BoundingBox(x0, -64, z0, x0 + CELL - 1, 320, z0 + CELL - 1);
            return boxes.stream().noneMatch(cell::intersects);
        }).count();
        info.add(String.format(Locale.ROOT, "Settlement land: %d free gentle land cells of %d blocks (%d in the map); target 30-60 settlements.",
                free, CELL, cells * cells));
    }

    /** Share of the land surface per biome on a 96² grid, to explain missing biome-bound structures. */
    private static void surfaceBiomes(ServerLevel level, WorldSize size, List<String> info) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int grid = 96;
        double step = size.side() / (double) grid;
        double half = size.side() / 2.0;
        Map<String, Long> counts = IntStream.range(0, grid * grid).parallel().mapToObj(i -> {
            int x = (int) (-half + (i % grid + 0.5) * step);
            int z = (int) (-half + (i / grid + 0.5) * step);
            int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
            if (generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) < generator.getSeaLevel()) return null;
            return generator.getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, random.sampler()).unwrapKey()
                    .map(k -> k.location().toString()).orElse("?");
        }).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.groupingBy(b -> b, java.util.stream.Collectors.counting()));
        long land = counts.values().stream().mapToLong(Long::longValue).sum();
        String list = counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> String.format(Locale.ROOT, "%s %.1f%%", e.getKey(), 100.0 * e.getValue() / Math.max(1, land)))
                .collect(java.util.stream.Collectors.joining(", "));
        info.add("Surface biomes on land (" + counts.size() + "): " + list + ".");
    }

    private static double distance(Start a, Start b) {
        return Math.hypot(a.entranceBox().getCenter().getX() - b.entranceBox().getCenter().getX(),
                a.entranceBox().getCenter().getZ() - b.entranceBox().getCenter().getZ());
    }

    private static String at(Start s) {
        return center(s.entranceBox());
    }

    private static String center(BoundingBox box) {
        return "(" + box.getCenter().getX() + ", " + box.maxY() + ", " + box.getCenter().getZ() + ")";
    }

    private static String toJson(WorldSize size, long seed, List<EntryResult> results, List<Issue> issues, List<String> info) {
        JsonObject root = new JsonObject();
        root.addProperty("size", size.id());
        root.addProperty("seed", seed);
        root.addProperty("critical", issues.stream().filter(i -> i.severity() == Severity.CRITICAL).count());
        root.addProperty("warnings", issues.stream().filter(i -> i.severity() == Severity.WARNING).count());
        JsonArray entries = new JsonArray();
        for (EntryResult r : results) {
            JsonObject e = new JsonObject();
            e.addProperty("id", r.entry().id().toString());
            e.addProperty("category", r.entry().category());
            e.addProperty("dimension", r.entry().dimension().toString());
            e.addProperty("found", r.found());
            e.addProperty("reachable", r.reachable());
            e.addProperty("kinds", r.kinds());
            if (r.min() != null) e.addProperty("min", r.min());
            e.addProperty("severity", r.entry().severity().name().toLowerCase(Locale.ROOT));
            entries.add(e);
        }
        root.add("entries", entries);
        JsonArray issueArray = new JsonArray();
        for (Issue i : issues) {
            JsonObject o = new JsonObject();
            o.addProperty("severity", i.severity().name().toLowerCase(Locale.ROOT));
            o.addProperty("entry", i.entry());
            o.addProperty("message", i.message());
            issueArray.add(o);
        }
        root.add("issues", issueArray);
        JsonArray infoArray = new JsonArray();
        info.forEach(infoArray::add);
        root.add("info", infoArray);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private static String toMarkdown(WorldSize size, long seed, List<EntryResult> results, List<Issue> issues, List<String> info) {
        StringBuilder out = new StringBuilder();
        long critical = issues.stream().filter(i -> i.severity() == Severity.CRITICAL).count();
        out.append("# World validation: ").append(size.id()).append(", seed ").append(seed).append("\n\n");
        out.append(critical).append(" critical, ").append(issues.size() - critical).append(" warnings.\n\n");
        out.append("| Entry | Category | Dimension | Inside border | Reachable | Min | Status |\n| --- | --- | --- | --- | --- | --- | --- |\n");
        for (EntryResult r : results) {
            boolean short_ = r.min() != null && r.reachable() < r.min();
            String status = size == WorldSize.TEST ? "-" : short_ ? r.entry().severity().name().toLowerCase(Locale.ROOT) : "ok";
            out.append(String.format(Locale.ROOT, "| %s | %s | %s | %d | %d | %s | %s |%n", r.entry().id(), r.entry().category(), r.entry().dimension().getPath(),
                    r.found(), r.reachable(), r.min() == null ? "-" : r.min().toString(), status));
        }
        out.append("\n## Issues\n\n");
        if (issues.isEmpty()) out.append("None.\n");
        for (Issue i : issues) out.append("- **").append(i.severity().name().toLowerCase(Locale.ROOT)).append("** ").append(i.entry()).append(": ")
                .append(i.message()).append('\n');
        out.append("\n## Other checks\n\n");
        for (String line : info) out.append("- ").append(line).append('\n');
        return out.toString();
    }
}
