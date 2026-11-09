package livingkingdoms.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
 * <p>By default only candidate chunks are loaded: chunks where at least one structure set's
 * placement allows a start ({@link StructurePlacement#isStructureChunk}), the same test vanilla
 * {@code ChunkGenerator.createStructures} applies, so no start can be missed. They are read at
 * {@code structure_starts} status. {@code -Dlivingkingdoms.scan=chunks} loads every chunk of the
 * area instead, as the M1-B census did; it is kept to check the candidate method against.
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
                 BoundingBox hangingPiece, BoundingBox entranceBox, String entrance) {
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
        long started = System.currentTimeMillis();
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        WorldBorder border = level.getWorldBorder();
        // Terrain is only meaningful in natural dimensions without a ceiling (not the Nether, not the End void).
        boolean terrain = level.dimensionType().natural() && !level.dimensionType().hasCeiling();
        int seaLevel = generator.getSeaLevel();

        boolean everyChunk = "chunks".equals(System.getProperty("livingkingdoms.scan"));
        List<long[]> chunks = everyChunk ? allChunks(area) : candidateChunks(level, area);

        // Chunks are requested in batches, so the server's worker threads generate them in parallel. Each
        // chunk of a batch holds our ticket at structure_starts level until the batch is read: the ticket
        // of a plain asynchronous request lives one tick, which cancels the generation.
        List<Map.Entry<Structure, StructureStart>> found = new ArrayList<>();
        long retried = 0;
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
                        retried++;
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
        long loaded = System.currentTimeMillis();
        // Noise sampling is thread-safe (vanilla samples it from worker threads).
        List<Start> starts = found.parallelStream().map(entry -> {
            ResourceLocation key = registry.getKey(entry.getKey());
            String id = key != null ? key.toString() : "unknown:unregistered";
            return measure(level, generator, random, border, terrain, seaLevel, id, entry.getKey(), entry.getValue());
        }).toList();
        long now = System.currentTimeMillis();
        String method = String.format(java.util.Locale.ROOT, "%s, %d reloaded, %d ms loading, %d ms measuring",
                everyChunk ? "every chunk" : "candidate chunks", retried, loaded - started, now - loaded);
        return new Result(starts, chunks.size(), area.chunks(), method, now - started);
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
        String entrance = "-";
        if (terrain && step.equals(GenerationStep.Decoration.SURFACE_STRUCTURES.getName())) {
            int wet = 0;
            int total = 0;
            for (BoundingBox piece : pieces) {
                // Small pieces (paths, shafts) exaggerate slope; buried pieces do not show the terrain.
                if (Math.max(piece.getXSpan(), piece.getZSpan()) < MIN_PIECE) continue;
                Footprint ground = footprint(generator, level, random, piece, seaLevel);
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
                }
                wet += ground.wet();
                total += ground.total();
            }
            water = total > 0 ? wet / (double) total : 0;
            entrance = entranceState(generator, level, random, entranceBox, seaLevel);
        }
        return new Start(id, step, structure.terrainAdaptation().getSerializedName(), biome, box, pieces, templates, inside, slope,
                relief, water, hanging, hangingPiece, entranceBox, entrance);
    }

    /** Template ids of jigsaw pieces (from the pool element), piece type ids of the others. */
    private static Set<String> templates(List<StructurePiece> pieces) {
        Set<String> result = new LinkedHashSet<>();
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece pooled) {
                // SinglePoolElement keeps its template id private; its toString is "Single[Left[ns:path]]".
                Matcher matcher = RESOURCE_ID.matcher(pooled.getElement().toString());
                while (matcher.find()) result.add(matcher.group());
            } else {
                ResourceLocation type = BuiltInRegistries.STRUCTURE_PIECE.getKey(piece.getType());
                if (type != null) result.add(type.toString());
            }
        }
        return result;
    }

    private static String biomeAt(ServerLevel level, ChunkGenerator generator, RandomState random, BoundingBox box) {
        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(box.getCenter().getX()),
                QuartPos.fromBlock(box.minY()), QuartPos.fromBlock(box.getCenter().getZ()), random.sampler());
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown:biome");
    }

    /**
     * Where the start piece (the entrance of a jigsaw structure) sits against the ground under it:
     * {@code buried} if its top is below the lowest ground, {@code floating} if its bottom is more
     * than {@value #FLOAT_GAP} blocks above the highest ground, otherwise {@code ok}.
     */
    static String entranceState(ChunkGenerator generator, ServerLevel level, RandomState random, BoundingBox entrance, int seaLevel) {
        Footprint ground = footprint(generator, level, random, entrance, seaLevel);
        if (entrance.maxY() < ground.lowest()) return "buried";
        if (entrance.minY() > ground.lowest() + ground.relief() + FLOAT_GAP) return "floating";
        return "ok";
    }

    /**
     * Ground under a box from the generator noise: relief, samples below sea level, samples where the
     * box bottom is more than {@value #FLOAT_GAP} blocks above the ground, all samples, lowest ground.
     */
    record Footprint(int relief, int wet, int hanging, int total, int lowest) {}

    static Footprint footprint(ChunkGenerator generator, ServerLevel level, RandomState random, BoundingBox box, int seaLevel) {
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
                if (box.minY() > height + FLOAT_GAP) hanging++;
                total++;
            }
        }
        return new Footprint(max - min, wet, hanging, total, min);
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
