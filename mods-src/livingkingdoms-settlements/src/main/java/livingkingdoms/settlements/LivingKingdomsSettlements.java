package livingkingdoms.settlements;

import com.mojang.logging.LogUtils;
import livingkingdoms.settlements.resident.Residents;
import livingkingdoms.settlements.work.Labor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entry point of Living Kingdoms Settlements: residents are MCA Reborn villagers, this mod owns
 * their settlement, profession, home and work (ADR-0001, ADR-0030).
 *
 * <p>MCA classes are named only in {@code livingkingdoms.settlements.mca}.
 */
@Mod(LivingKingdomsSettlements.MOD_ID)
public final class LivingKingdomsSettlements {
    public static final String MOD_ID = "livingkingdoms_settlements";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdomsSettlements(IEventBus modEventBus) {
        Residents.ATTACHMENT_TYPES.register(modEventBus);
        Labor.ACTIVITIES.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(Residents::onDeath);
        NeoForge.EVENT_BUS.addListener(SettlementCommand::register);
    }
}
