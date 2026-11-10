package livingkingdoms.settlements.mca.mixin;

import livingkingdoms.settlements.mca.McaAdapter;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Our only patch of MCA Reborn (ADR-0030). MCA builds every villager brain in this one method, on
 * creation and on each refresh, and offers no event for it; for a resident our adapter builds the
 * brain instead.
 */
@Mixin(value = VillagerTasksMCA.class, remap = false)
abstract class VillagerTasksMcaMixin {
    @Inject(method = "initializeTasks", at = @At("HEAD"), cancellable = true)
    private static void livingkingdoms$buildResidentBrain(VillagerEntityMCA villager, Brain<VillagerEntityMCA> brain,
            CallbackInfoReturnable<Brain<VillagerEntityMCA>> callback) {
        if (McaAdapter.buildResidentBrain(villager, brain)) {
            callback.setReturnValue(brain);
        }
    }
}
