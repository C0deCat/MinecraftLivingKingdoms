package livingkingdoms.world;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point of Living Kingdoms World: fixed world sizes, per-dimension borders and the
 * finite-world edge (ADR-0004). Client-only code lives in {@code livingkingdoms.world.client}.
 */
@Mod(LivingKingdomsWorld.MOD_ID)
public final class LivingKingdomsWorld {
    public static final String MOD_ID = "livingkingdoms_world";
    static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdomsWorld(IEventBus modEventBus) {
        LOGGER.info("Living Kingdoms World loaded");
    }
}
