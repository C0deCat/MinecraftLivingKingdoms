package livingkingdoms.settlements.mca;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import livingkingdoms.settlements.resident.Residents;
import livingkingdoms.settlements.work.Labor;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.VillagerFactory;
import net.conczin.mca.entity.ai.ActivitiesMCA;
import net.conczin.mca.entity.ai.MemoryModuleTypeMCA;
import net.conczin.mca.entity.ai.brain.VillagerTasksMCA;
import net.conczin.mca.entity.ai.relationship.AgeState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;

/**
 * The only place that names MCA Reborn classes (ARCHITECTURE.md): an MCA update is fixed here and
 * in the Mixin next to it. Facts about MCA: docs/references/mca-reborn-7.7.md.
 */
public final class McaAdapter {
    private static final float SPEED = 0.5F;

    private McaAdapter() {}

    /** The entity as a resident's body, if it is an MCA villager. */
    public static Optional<Villager> body(Entity entity) {
        return entity instanceof VillagerEntityMCA villager ? Optional.of(villager) : Optional.empty();
    }

    /** Rebuilds the brain so a change of resident status or order takes effect now. */
    public static void refreshBrain(Villager body) {
        if (body instanceof VillagerEntityMCA villager && villager.level() instanceof ServerLevel level) {
            villager.refreshBrain(level);
        }
    }

    /** Spawns an adult MCA villager the way MCA does (name, gender, family tree entry). */
    public static Villager spawnAdult(ServerLevel level, Vec3 position) {
        return VillagerFactory.newVillager(level).withPosition(position).withAge(0).spawn(MobSpawnType.COMMAND);
    }

    /** Name of the activity the brain is in, for status output. */
    public static String activityName(Villager body) {
        return body.getBrain().getActiveNonCoreActivity().map(Activity::getName).orElse("none");
    }

    /**
     * Builds the brain of a resident from MCA's public task packages plus our labor activity, and
     * leaves out MCA's job-site package so the resident neither takes nor loses a profession by
     * itself. Called from the Mixin on every brain build.
     *
     * @return false when MCA should build the brain: not a resident, a resident without work, a child, a guard, or a
     *         villager told by a player to follow or stay
     */
    public static boolean buildResidentBrain(VillagerEntityMCA villager, Brain<VillagerEntityMCA> brain) {
        if (!Residents.isResident(villager) || !Residents.hasWork(villager)) return false;
        if (brain.getMemoryInternal(MemoryModuleTypeMCA.STAYING).isPresent()) return false;
        if (brain.getMemoryInternal(MemoryModuleTypeMCA.PLAYER_FOLLOWING).isPresent()) return false;
        if (AgeState.byCurrentAge(villager.getAge()) != AgeState.ADULT) return false;
        if (villager.isGuard() || !villager.requiresHome()) return false;

        brain.setSchedule(Labor.schedule());
        brain.addActivity(Activity.CORE, VillagerTasksMCA.getImportantCorePackage(SPEED));
        brain.addActivity(Activity.CORE, VillagerTasksMCA.getCorePackage(SPEED));
        brain.addActivity(Activity.CORE, VillagerTasksMCA.getSelfDefencePackage());
        brain.addActivity(Labor.ACTIVITY.get(), Labor.tasks(SPEED));
        brain.addActivityWithConditions(Activity.MEET, VillagerTasksMCA.getMeetPackage(SPEED),
                ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivityWithConditions(Activity.REST, VillagerTasksMCA.getRestPackage(SPEED),
                ImmutableSet.of(Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT)));
        brain.addActivity(Activity.IDLE, VillagerTasksMCA.getIdlePackage(SPEED));
        brain.addActivity(Activity.PANIC, VillagerTasksMCA.getPanicPackage(SPEED));
        brain.addActivity(Activity.PRE_RAID, VillagerTasksMCA.getPreRaidPackage(SPEED));
        brain.addActivity(Activity.RAID, VillagerTasksMCA.getRaidPackage(SPEED));
        brain.addActivity(Activity.HIDE, VillagerTasksMCA.getHidePackage(SPEED));
        brain.addActivity(ActivitiesMCA.GRIEVE, VillagerTasksMCA.getGrievingPackage());

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(villager.level().getDayTime(), villager.level().getGameTime());
        return true;
    }
}
