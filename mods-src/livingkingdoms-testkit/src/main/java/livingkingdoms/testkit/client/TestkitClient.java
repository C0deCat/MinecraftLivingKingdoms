package livingkingdoms.testkit.client;

import livingkingdoms.testkit.LivingKingdomsTestkit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point: starts the e2e driver's socket once the client is set up. */
@Mod(value = LivingKingdomsTestkit.MOD_ID, dist = Dist.CLIENT)
public final class TestkitClient {
    public TestkitClient(IEventBus modBus) {
        TickTasks.register(NeoForge.EVENT_BUS);
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            OpRegistry ops = new OpRegistry();
            SessionOps.register(ops);
            WorldOps.register(ops);
            InputOps.register(ops);
            PlayerOps.register(ops);
            ContainerOps.register(ops);
            CommandOps.register(ops);
            QueryOps.register(ops);
            TestOps.register(ops);
            new Bridge(ops, Integer.getInteger("livingkingdoms.testkit.port", Bridge.DEFAULT_PORT)).start();
        });
    }
}
