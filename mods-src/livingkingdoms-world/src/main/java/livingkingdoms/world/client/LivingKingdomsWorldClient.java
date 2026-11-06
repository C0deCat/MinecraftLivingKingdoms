package livingkingdoms.world.client;

import livingkingdoms.world.LivingKingdomsWorld;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point: never loaded on a dedicated server. */
@Mod(value = LivingKingdomsWorld.MOD_ID, dist = Dist.CLIENT)
public final class LivingKingdomsWorldClient {
    public LivingKingdomsWorldClient() {
        NeoForge.EVENT_BUS.addListener(CreateWorldHooks::onInit);
        NeoForge.EVENT_BUS.addListener(CreateWorldHooks::onRender);
    }
}
