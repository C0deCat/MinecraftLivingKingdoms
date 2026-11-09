package livingkingdoms.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Collects the structure starts of one dimension in an area and measures them. Shared by the
 * census ({@link StructureCensus}) and the validator.
 *
 * <p>Three ways to find the starts, chosen with {@code -Dlivingkingdoms.scan}:
 * <ul>
 *   <li>{@code direct} (default): starts are computed in parallel without loading chunks, by the same
 *       placement test ({@link StructurePlacement#isStructureChunk}), selection and generation as
 *       vanilla {@code ChunkGenerator.createStructures};</li>
 *   <li>{@code candidates}: the chunks that pass the placement test are loaded at
 *       {@code structure_starts} status and their starts read; vanilla generates starts one chunk at
 *       a time, so this costs about 46 ms per chunk;</li>
 *   <li>{@code chunks}: every chunk of the area is loaded, as the M1-B census did.</li>
 * </ul>
 * The two loading methods are kept to check {@code direct} against.
 *
 * <p>Surface structures in dimensions without a ceiling also get the terrain under their pieces,
 * sampled from the generator noise (so the structure's own blocks do not count): the worst piece
 * slope (relief under one piece divided by its longer side), the share of ground below sea level,
 * the worst share of a piece's footprint that hangs more than {@value #FLOAT_GAP} blocks above the
 * ground, and the state of the entrance (the start piece). Only pieces at least {@value #MIN_PIECE}
 * blocks long whose top reaches the lowest ground under them count, so buried rooms, shafts and
 * path tiles do not.
 */
final class StructureScan {
    static final int SAMPLE_STEP = 4;
    static final int MIN_PIECE = 8;
    static final int FLOAT_GAP = 3;
    private static final int BATCH = 64;
    private static final TicketType<ChunkPos> SCAN_TICKET = TicketType.create("livingkingdoms_scan", Comparator.comparingLong(ChunkPos::toLong));
    /** Region ticket distance for structure_starts: a level above FULL, so the distance is negative. */
    private static final int TICKET_DISTANCE = ChunkLevel.byStatus(FullChunkStatus.FULL) - ChunkLevel.byStatus(ChunkStatus.STRUCTURE_STARTS);
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_/.-]+");

    private StructureScan() {}

    /** Chunk range of a scan, inclusive. */
    record Area(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, String label) {
        static Area square(int radius) {
            int r = Math.floorDiv(radius, 16);
            return new Area(-r, r, -r, r, "radius " + radius);
        }

        /** Every chunk that touches the inside of the level's border. */
        static Area border(ServerLevel level) {
            WorldBorder border = level.getWorldBorder();
            return new Area(Math.floorDiv((int) Math.floor(border.getMinX()), 16), Math.floorDiv((int) Math.ceil(border.getMaxX()) - 1, 16),
                    Math.floorDiv((int) Math.floor(border.getMinZ()), 16), Math.floorDiv((int) Math.ceil(border.getMaxZ()) - 1, 16),
                    "border " + (int) border.getSize());
        }

        long chunks() {
            return (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        }
    }

    /**
     * One structure start. Terrain fields are -1 (and {@code entrance} is {@code "-"}) when the start
     * was not measured: not a surface structure, a dimension with a ceiling or without terrain, or no
     * piece that reaches the ground.
     */
    record Start(String id, String step, String adaptation, String biome, BoundingBox box, List<BoundingBox> pieces,
                 Set<String> templates, boolean inside, double slope, int relief, double water, double hanging,
                 BoundingBox hangingPiece, String hangingTemplate, BoundingBox entranceBox, String entrance) {
        String namespace() {
            return id.substring(0, id.indexOf(':'));
        }

        boolean surface() {
            return step.equals(GenerationStep.Decoration.SURFACE_STRUCTURES.getName());
        }

        boolean measured() {
            return !entrance.equals("-");
        }

        /** Terrain adaptation that fills or carves around the structure (vanilla beardifier). */
        boolean adapted() {
            return !adaptation.equals(TerrainAdjustment.NONE.getSerializedName());
        }
    }

    record Overlap(Start a, Start b, long volume) {}

    record Result(List<Start> starts, long chunksLoaded, long chunksInArea, String method, long millis) {}

    /** Runs off the server thread; chunk loads are handed to the server thread. */
    static Result scan(ServerLevel level, Area area) {
        return scan(level, area, id -> true);
    }

    /** As {@link #scan(ServerLevel, Area)}, measuring the terrain only of starts whose id passes {@code measured}. */
    static Result scan(ServerLevel level, Area area, java.util.function.Predicate<String> measured) {
        long started = System.currentTimeMillis();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        WorldBorder border = level.getWorldBorder();
        // Terrain is only meaningful in natural dimensions without a ceiling (not the Nether, not the End void).
        boolean terrain = level.dimensionType().natural() && !level.dimensionType().hasCeiling();
        int seaLevel = generator.getSeaLevel();

        String mode = System.getProperty("livingkingdoms.scan", "direct");
        List<Map.Entry<Structure, StructureStart>> found;
        long loadedChunks;
        String label;
        if (mode.equals("direct")) {
            AtomicLong attempts = new AtomicLong();
            found = direct(level, area, attempts);
            loadedChunks = 0;
            label = "direct, " + attempts.get() + " candidate chunks";
        } else {
            boolean everyChunk = mode.equals("chunks");
            List<long[]> chunks = everyChunk ? allChunks(area) : candidateChunks(level, area);
            found = loadStarts(level, chunks);
            loadedChunks = chunks.size();
            label = everyChunk ? "every chunk" : "candidate chunks";
        }
        long loaded = System.currentTimeMillis();
        // Noise sampling is thread-safe (vanilla samples it from worker threads).
        List<Start> starts = found.parallelStream().map(entry -> {
            ResourceLocation key = registry.getKey(entry.getKey());
            String id = key != null ? key.toString() : "unknown:unregistered";
            return measure(level, generator, random, border, terrain && measured.test(id), seaLevel, id, entry.getKey(), entry.getValue());
        }).toList();
        long now = System.currentTimeMillis();
        String method = String.format(java.util.Locale.ROOT, "%s, %d ms finding starts, %d ms measuring", label, loaded - started, now - loaded);
        return new Result(starts, loadedChunks, area.chunks(), method, now - started);
    }

    /**
     * Starts computed without loading chunks, in parallel: every candidate chunk gets a throwaway
     * {@link ProtoChunk} and vanilla {@link ChunkGenerator#createStructures} runs on it, so the
     * selection, generation and other mods' hooks into it (YUNG's and Moog's disable vanilla
     * structures there) are exactly those of real worldgen. The world keeps no trace of the run.
     *
     * <p>Vanilla generates structure starts one chunk at a time, and some mods rely on it: the Twilight
     * Forest's knight stronghold keeps shared state and can throw when two run at once, and its and
     * Mowzie's monastery's piece layouts may come out slightly different (same starts). A chunk that
     * throws is generated again afterwards, one at a time.
     */
    private static List<Map.Entry<Structure, StructureStart>> direct(ServerLevel level, Area area, AtomicLong attempts) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        StructureTemplateManager templates = level.getStructureManager();
        StructureManager structures = level.structureManager();
        RegistryAccess registries = level.registryAccess();
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        List<long[]> chunks = candidateChunks(level, area);
        attempts.set(chunks.size());
        Queue<Map.Entry<Structure, StructureStart>> found = new ConcurrentLinkedQueue<>();
        Queue<long[]> failed = new ConcurrentLinkedQueue<>();
        java.util.function.Consumer<long[]> generate = pos -> {
            ProtoChunk chunk = new ProtoChunk(new ChunkPos((int) pos[0], (int) pos[1]), UpgradeData.EMPTY, level, biomes, null);
            generator.createStructures(registries, state, structures, chunk, templates);
            for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                if (entry.getValue().isValid()) found.add(Map.entry(entry.getKey(), entry.getValue()));
            }
        };
        chunks.parallelStream().forEach(pos -> {
            try {
                generate.accept(pos);
            } catch (RuntimeException e) {
                failed.add(pos);
            }
        });
        for (long[] pos : failed) {
            try {
                generate.accept(pos);
            } catch (RuntimeException e) {
                LivingKingdomsWorld.LOGGER.error("Structure starts of chunk {}, {} in {} failed", pos[0], pos[1], level.dimension().location(), e);
            }
        }
        if (!failed.isEmpty()) {
            LivingKingdomsWorld.LOGGER.info("Structure scan of {}: {} chunks generated again one at a time", level.dimension().location(), failed.size());
        }
        return new ArrayList<>(found);
    }

    /** Starts read from the chunks, loaded at {@code structure_starts} status. */
    private static List<Map.Entry<Structure, StructureStart>> loadStarts(ServerLevel level, List<long[]> chunks) {
        // Chunks are requested in batches. Each chunk of a batch holds our ticket at structure_starts level
        // until the batch is read: the ticket of a plain asynchronous request lives one tick, which cancels
        // the generation. Vanilla generates structure starts one chunk at a time, so this is still slow.
        List<Map.Entry<Structure, StructureStart>> found = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += BATCH) {
            List<ChunkPos> slice = chunks.subList(i, Math.min(chunks.size(), i + BATCH)).stream()
                    .map(pos -> new ChunkPos((int) pos[0], (int) pos[1])).toList();
            level.getServer().submit(() -> slice.forEach(pos -> level.getChunkSource().addRegionTicket(SCAN_TICKET, pos, TICKET_DISTANCE, pos))).join();
            try {
                List<CompletableFuture<ChunkResult<ChunkAccess>>> batch = new ArrayList<>();
                for (ChunkPos pos : slice) batch.add(level.getChunkSource().getChunkFuture(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS, true));
                for (int j = 0; j < slice.size(); j++) {
                    ChunkAccess chunk = batch.get(j).join().orElse(null);
                    if (chunk == null) {
                        // Off the server thread this call hands the load to the server thread and waits for it.
                        chunk = level.getChunkSource().getChunk(slice.get(j).x, slice.get(j).z, ChunkStatus.STRUCTURE_STARTS, true);
                    }
                    if (chunk == null) continue;
                    for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                        if (entry.getValue().isValid()) found.add(Map.entry(entry.getKey(), entry.getValue()));
                    }
                }
            } finally {
                level.getServer().execute(() -> slice.forEach(pos -> level.getChunkSource().removeRegionTicket(SCAN_TICKET, pos, TICKET_DISTANCE, pos)));
            }
        }
        return found;
    }

    private static List<long[]> allChunks(Area area) {
        List<long[]> result = new ArrayList<>();
        for (int cx = area.minChunkX(); cx <= area.maxChunkX(); cx++) {
            for (int cz = area.minChunkZ(); cz <= area.maxChunkZ(); cz++) result.add(new long[] {cx, cz});
        }
        return result;
    }

    /** Chunks of the area where any structure set of this level may start a structure. */
    private static List<long[]> candidateChunks(ServerLevel level, Area area) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        state.ensureStructuresGenerated();
        Set<StructurePlacement> unique = new LinkedHashSet<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) unique.add(set.value().placement());
        List<StructurePlacement> placements = List.copyOf(unique);
        List<long[]> result = new ArrayList<>();
        for (int cx = area.minChunkX(); cx <= area.maxChunkX(); cx++) {
            for (int cz = area.minChunkZ(); cz <= area.maxChunkZ(); cz++) {
                for (StructurePlacement placement : placements) {
                    if (placement.isStructureChunk(state, cx, cz)) {
                        result.add(new long[] {cx, cz});
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static Start measure(ServerLevel level, ChunkGenerator generator, RandomState random, WorldBorder border, boolean terrain,
                                 int seaLevel, String id, Structure structure, StructureStart start) {
        String step = structure.step().getName();
        BoundingBox box = start.getBoundingBox();
        List<StructurePiece> rawPieces = start.getPieces();
        List<BoundingBox> pieces = rawPieces.stream().map(StructurePiece::getBoundingBox).toList();
        Set<String> templates = templates(rawPieces);
        boolean inside = border.isWithinBounds(box.minX(), box.minZ()) && border.isWithinBounds(box.maxX(), box.maxZ());
        BoundingBox entranceBox = pieces.isEmpty() ? box : pieces.get(0);
        String biome = biomeAt(level, generator, random, entranceBox);
        double slope = -1;
        int relief = -1;
        double water = -1;
        double hanging = -1;
        BoundingBox hangingPiece = entranceBox;
        String hangingTemplate = "-";
        String entrance = "-";
        if (terrain && step.equals(GenerationStep.Decoration.SURFACE_STRUCTURES.getName())) {
            int wet = 0;
            int total = 0;
            for (int i = 0; i < pieces.size(); i++) {
                BoundingBox piece = pieces.get(i);
                // Small pieces (paths, shafts) exaggerate slope; buried pieces do not show the terrain.
                if (Math.max(piece.getXSpan(), piece.getZSpan()) < MIN_PIECE) continue;
                Footprint ground = footprint(generator, level, random, piece, seaLevel, pieces);
                if (piece.maxY() < ground.lowest()) continue;
                double pieceSlope = ground.relief() / (double) Math.max(1, Math.max(piece.getXSpan(), piece.getZSpan()));
                if (pieceSlope > slope) {
                    slope = pieceSlope;
                    relief = ground.relief();
                }
                double pieceHanging = ground.hanging() / (double) ground.total();
                if (pieceHanging > hanging) {
                    hanging = pieceHanging;
                    hangingPiece = piece;
                    hangingTemplate = template(rawPieces.get(i));
                }
                wet += ground.wet();
                total += ground.total();
            }
            water = total > 0 ? wet / (double) total : 0;
            entrance = entranceState(generator, level, random, entranceBox, seaLevel, pieces);
        }
        return new Start(id, step, structure.terrainAdaptation().getSerializedName(), biome, box, pieces, templates, inside, slope,
                relief, water, hanging, hangingPiece, hangingTemplate, entranceBox, entrance);
    }

    /**
     * Whether a freshly generated start stands on the ground: true for structures that are not surface
     * structures or not in a dimension with terrain; otherwise its entrance must not float and no piece
     * may hang over more than {@link StructureCensus#HANGING_SHARE} of its footprint. Used by the
     * {@code fixed_count} placement to reject points on cliffs.
     */
    static boolean grounded(ServerLevel level, Structure structure, StructureStart start) {
        boolean terrain = level.dimensionType().natural() && !level.dimensionType().hasCeiling();
        if (!terrain || structure.step() != GenerationStep.Decoration.SURFACE_STRUCTURES) return true;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        Start s = measure(level, generator, level.getChunkSource().randomState(), level.getWorldBorder(), true, generator.getSeaLevel(),
                "check:start", structure, start);
        return !s.entrance().equals("floating") && s.hanging() <= StructureCensus.HANGING_SHARE;
    }

    /** Template ids of jigsaw pieces (from the pool element), piece type ids of the others. */
    private static Set<String> templates(List<StructurePiece> pieces) {
        Set<String> result = new LinkedHashSet<>();
        for (StructurePiece piece : pieces) {
            String id = template(piece);
            if (!id.equals("-")) result.add(id);
        }
        return result;
    }

    /** Template id of a jigsaw piece (from the pool element), piece type id of the others, "-" if unknown. */
    private static String template(StructurePiece piece) {
        if (piece instanceof PoolElementStructurePiece pooled) {
            // SinglePoolElement keeps its template id private; its toString is "Single[Left[ns:path]]".
            Matcher matcher = RESOURCE_ID.matcher(pooled.getElement().toString());
            return matcher.find() ? matcher.group() : "-";
        }
        ResourceLocation type = BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
        return type != null ? type.toString() : "-";
    }

    private static String biomeAt(ServerLevel level, ChunkGenerator generator, RandomState random, BoundingBox box) {
        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(box.getCenter().getX()),
                QuartPos.fromBlock(box.minY()), QuartPos.fromBlock(box.getCenter().getZ()), random.sampler());
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown:biome");
    }

    /**
     * Where the start piece (the entrance of a jigsaw structure) sits against the ground under it:
     * {@code buried} if its top is below the lowest ground, {@code floating} if more than half of its
     * footprint hangs (see {@link #footprint}), otherwise {@code ok}.
     */
    static String entranceState(ChunkGenerator generator, ServerLevel level, RandomState random, BoundingBox entrance, int seaLevel,
                                List<BoundingBox> pieces) {
        Footprint ground = footprint(generator, level, random, entrance, seaLevel, pieces);
        if (entrance.maxY() < ground.lowest()) return "buried";
        if (ground.hanging() * 2 > ground.total()) return "floating";
        return "ok";
    }

    /**
     * Ground under a box from the generator noise: relief, samples below sea level, samples where the
     * box hangs, all samples, lowest ground. A sample hangs where the box bottom is more than
     * {@value #FLOAT_GAP} blocks above the ground and no other piece of the same structure lies right
     * under it: the floors of a tower are separate jigsaw pieces standing on each other.
     */
    record Footprint(int relief, int wet, int hanging, int total, int lowest) {}

    static Footprint footprint(ChunkGenerator generator, ServerLevel level, RandomState random, BoundingBox box, int seaLevel) {
        return footprint(generator, level, random, box, seaLevel, List.of());
    }

    static Footprint footprint(ChunkGenerator generator, ServerLevel level, RandomState random, BoundingBox box, int seaLevel,
                               List<BoundingBox> pieces) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int wet = 0;
        int hanging = 0;
        int total = 0;
        for (int x = box.minX(); x <= box.maxX(); x += SAMPLE_STEP) {
            for (int z = box.minZ(); z <= box.maxZ(); z += SAMPLE_STEP) {
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                min = Math.min(min, height);
                max = Math.max(max, height);
                if (height < seaLevel) wet++;
                // The heightmap is the first free block above the ground.
                if (box.minY() > height + FLOAT_GAP && !supported(box, pieces, x, z)) hanging++;
                total++;
            }
        }
        return new Footprint(max - min, wet, hanging, total, min);
    }

    /** Whether another piece's top lies within {@value #FLOAT_GAP} blocks under the box bottom at this column. */
    private static boolean supported(BoundingBox box, List<BoundingBox> pieces, int x, int z) {
        for (BoundingBox other : pieces) {
            if (other == box || x < other.minX() || x > other.maxX() || z < other.minZ() || z > other.maxZ()) continue;
            if (other.maxY() < box.minY() + 1 && other.maxY() >= box.minY() - 1 - FLOAT_GAP) return true;
            // Overlapping boxes (a piece reaching into the one above it) also carry it.
            if (other.minY() < box.minY() && other.maxY() >= box.minY()) return true;
        }
        return false;
    }

    /** Intersecting pairs of starts, found with a sweep over minX; volume counts intersecting pieces only. */
    static List<Overlap> overlaps(List<Start> starts) {
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
}
