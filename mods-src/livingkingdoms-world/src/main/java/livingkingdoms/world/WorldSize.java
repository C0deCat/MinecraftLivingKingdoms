package livingkingdoms.world;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * The four fixed world sizes (ADR-0004). The side includes the outer ocean ring.
 *
 * <p>Each size has its own world preset; the presets differ only in the Nether dimension type, so the
 * size of a save is read back from the dimension type of its Nether level.
 */
public enum WorldSize {
    TEST("test", 2_048, 4, "livingkingdoms:the_nether_test"),
    SMALL("small", 8_192, 4, "livingkingdoms:the_nether_small"),
    MEDIUM("medium", 12_288, 6, "livingkingdoms:the_nether_medium"),
    LARGE("large", 16_384, 8, "minecraft:the_nether");

    /** Preselected on the creation screen and assumed for saves without one of our Nether types. */
    public static final WorldSize DEFAULT = LARGE;

    private final String id;
    private final int side;
    private final int netherRatio;
    private final ResourceKey<DimensionType> netherType;

    WorldSize(String id, int side, int netherRatio, String netherType) {
        this.id = id;
        this.side = side;
        this.netherRatio = netherRatio;
        this.netherType = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.parse(netherType));
    }

    public String id() {
        return id;
    }

    /** Overworld side in blocks; also the End and Twilight Forest side (both 1:1). */
    public int side() {
        return side;
    }

    public int netherRatio() {
        return netherRatio;
    }

    public int netherSide() {
        return side / netherRatio;
    }

    public ResourceLocation presetId() {
        return ResourceLocation.fromNamespaceAndPath("livingkingdoms", id);
    }

    public static Optional<WorldSize> byId(String id) {
        for (WorldSize size : values()) {
            if (size.id.equals(id)) return Optional.of(size);
        }
        return Optional.empty();
    }

    public static Optional<WorldSize> byNetherType(ResourceKey<DimensionType> type) {
        for (WorldSize size : values()) {
            if (size.netherType.equals(type)) return Optional.of(size);
        }
        return Optional.empty();
    }

    /** Reads the size of the running save. Empty if the save has no Nether or an unknown Nether type. */
    public static Optional<WorldSize> of(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return Optional.empty();
        return nether.dimensionTypeRegistration().unwrapKey().flatMap(WorldSize::byNetherType);
    }
}
