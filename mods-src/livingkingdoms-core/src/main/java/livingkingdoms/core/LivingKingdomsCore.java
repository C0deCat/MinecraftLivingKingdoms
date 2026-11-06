package livingkingdoms.core;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point of Living Kingdoms Core.
 *
 * <p>Core depends only on NeoForge and will own stable IDs, event history, regions, time and
 * unloaded-world state (see ARCHITECTURE.md). M0 ships it empty on purpose.
 */
@Mod(LivingKingdomsCore.MOD_ID)
public final class LivingKingdomsCore {
    public static final String MOD_ID = "livingkingdoms_core";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdomsCore(IEventBus modEventBus) {
        LOGGER.info("Living Kingdoms Core loaded");
    }
}
