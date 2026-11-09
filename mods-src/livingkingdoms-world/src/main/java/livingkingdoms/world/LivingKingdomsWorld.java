package livingkingdoms.world;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Entry point of Living Kingdoms World: fixed world sizes, per-dimension borders and the
 * finite-world edge (ADR-0004). Client-only code lives in {@code livingkingdoms.world.client}.
 */
@Mod(LivingKingdomsWorld.MOD_ID)
public final class LivingKingdomsWorld {
    public static final String MOD_ID = "livingkingdoms_world";
    static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MOD_ID);

    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSOR_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MOD_ID);

    static {
        DENSITY_FUNCTION_TYPES.register("edge_falloff", () -> EdgeFalloff.MAP_CODEC);
        DENSITY_FUNCTION_TYPES.register("edge_bias", () -> EdgeBias.MAP_CODEC);
        DENSITY_FUNCTION_TYPES.register("edge_erosion", () -> EdgeErosion.MAP_CODEC);
        STRUCTURE_PROCESSOR_TYPES.register("foundation", () -> FoundationProcessor.TYPE);
    }

    public LivingKingdomsWorld(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        STRUCTURE_PROCESSOR_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(WorldSizeState::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(WorldBorders::onServerStarted);
        NeoForge.EVENT_BUS.addListener(BorderSync::onLoggedIn);
        NeoForge.EVENT_BUS.addListener(BorderSync::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(BorderSync::onRespawn);
        NeoForge.EVENT_BUS.addListener(WorldMapCommand::register);
    }
}
