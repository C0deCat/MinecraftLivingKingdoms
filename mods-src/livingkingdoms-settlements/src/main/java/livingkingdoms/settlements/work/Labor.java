package livingkingdoms.settlements.work;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.VillageBoundRandomStroll;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The working part of a resident's day: our own brain activity, the schedule that contains it and
 * the tasks that run during it. Residents do not use the vanilla work activity, which is tied to
 * job-site blocks (ADR-0030).
 */
public final class Labor {
    public static final DeferredRegister<Activity> ACTIVITIES =
            DeferredRegister.create(Registries.ACTIVITY, LivingKingdomsSettlements.MOD_ID);

    public static final DeferredHolder<Activity, Activity> ACTIVITY =
            ACTIVITIES.register("labor", () -> new Activity(LivingKingdomsSettlements.MOD_ID + ":labor"));

    /** Start of labor in day ticks; the rest of the day follows MCA's default villager schedule. */
    public static final int START = 2000;
    public static final int END = 9000;

    private static final Supplier<Schedule> SCHEDULE = Suppliers.memoize(() -> new ScheduleBuilder(new Schedule())
            .changeActivityAt(10, Activity.IDLE)
            .changeActivityAt(START, ACTIVITY.get())
            .changeActivityAt(END, Activity.MEET)
            .changeActivityAt(11000, Activity.IDLE)
            .changeActivityAt(12500, Activity.REST)
            .build());

    private Labor() {}

    public static Schedule schedule() {
        return SCHEDULE.get();
    }

    /** The tasks of the labor activity, written against vanilla types so only the adapter names MCA. */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> tasks(float speed) {
        return ImmutableList.of(
                Pair.of(2, new HarvestAndDeliver()),
                Pair.of(7, VillageBoundRandomStroll.create(speed)),
                Pair.of(99, UpdateActivityFromSchedule.create()));
    }
}
