package livingkingdoms.settlements;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Optional;
import livingkingdoms.settlements.mca.McaAdapter;
import livingkingdoms.settlements.resident.ResidentRecord;
import livingkingdoms.settlements.resident.Residents;
import livingkingdoms.settlements.settlement.SettlementRecord;
import livingkingdoms.settlements.settlement.Settlements;
import livingkingdoms.settlements.survey.VillageSurvey;
import livingkingdoms.settlements.work.HarvestOrder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /lksettle}: debug commands of the settlement mod (operators only).
 *
 * <ul>
 *   <li>{@code spawn <pos>} spawns an adult MCA villager the way MCA does, for tests and trials;</li>
 *   <li>{@code assign <villager> harvest <field> <storage> [radius]} makes an MCA villager a
 *       resident with a harvest order;</li>
 *   <li>{@code dismiss <villager>} makes it an ordinary MCA villager again;</li>
 *   <li>{@code status} lists the settlements and the resident records, loaded or not;</li>
 *   <li>{@code survey [max]} measures the villages the world generates ({@link VillageSurvey}).</li>
 * </ul>
 */
final class SettlementCommand {
    private static final SimpleCommandExceptionType NOT_MCA_VILLAGER = new SimpleCommandExceptionType(
            Component.literal("That entity is not an MCA Reborn villager; only those can be residents"));
    private static final SimpleCommandExceptionType NOT_RESIDENT = new SimpleCommandExceptionType(
            Component.literal("That villager is not a resident"));

    private SettlementCommand() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lksettle")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(SettlementCommand::spawn)))
                .then(Commands.literal("assign")
                        .then(Commands.argument("villager", EntityArgument.entity())
                                .then(Commands.literal("harvest")
                                        .then(Commands.argument("field", BlockPosArgument.blockPos())
                                                .then(Commands.argument("storage", BlockPosArgument.blockPos())
                                                        .executes(context -> assignHarvest(context, HarvestOrder.DEFAULT_RADIUS))
                                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, HarvestOrder.MAX_RADIUS))
                                                                .executes(context -> assignHarvest(context, IntegerArgumentType.getInteger(context, "radius")))))))))
                .then(Commands.literal("dismiss")
                        .then(Commands.argument("villager", EntityArgument.entity())
                                .executes(SettlementCommand::dismiss)))
                .then(Commands.literal("status")
                        .executes(SettlementCommand::status))
                .then(Commands.literal("survey")
                        .executes(context -> VillageSurvey.start(context.getSource(), Integer.MAX_VALUE))
                        .then(Commands.argument("max", IntegerArgumentType.integer(1))
                                .executes(context -> VillageSurvey.start(context.getSource(), IntegerArgumentType.getInteger(context, "max"))))));
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        Villager villager = McaAdapter.spawnAdult(context.getSource().getLevel(), Vec3.atBottomCenterOf(pos));
        context.getSource().sendSuccess(() -> Component.literal("Spawned " + villager.getName().getString() + " at " + pos.toShortString()), true);
        return 1;
    }

    private static int assignHarvest(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        Villager body = body(context);
        BlockPos field = BlockPosArgument.getBlockPos(context, "field");
        BlockPos storage = BlockPosArgument.getBlockPos(context, "storage");
        Residents.setOrder(body, Optional.of(new HarvestOrder(field, radius, storage)));
        McaAdapter.refreshBrain(body);
        ResidentRecord record = Residents.recordOf(body).orElseThrow();
        context.getSource().sendSuccess(() -> Component.literal(record.id() + " (" + record.name() + ") harvests around "
                + field.toShortString() + " and delivers to " + storage.toShortString()), true);
        return 1;
    }

    private static int dismiss(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Villager body = body(context);
        if (!Residents.dismiss(body)) throw NOT_RESIDENT.create();
        McaAdapter.refreshBrain(body);
        context.getSource().sendSuccess(() -> Component.literal(body.getName().getString() + " is no longer a resident"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<SettlementRecord> settlements = Settlements.all(source.getServer());
        source.sendSuccess(() -> Component.literal("Settlements: " + settlements.size()), false);
        for (SettlementRecord settlement : settlements) {
            long residents = Residents.countIn(source.getServer(), settlement.id());
            source.sendSuccess(() -> Component.literal(settlement.id() + " source=" + settlement.source() + " centre="
                    + settlement.centre().toShortString() + " residents=" + residents), false);
        }
        Collection<ResidentRecord> records = Residents.all(source.getServer());
        source.sendSuccess(() -> Component.literal("Residents: " + records.size()), false);
        for (ResidentRecord record : records) {
            Entity entity = findLoaded(source, record);
            String order = record.order()
                    .map(o -> "harvest field=" + o.field().toShortString() + " radius=" + o.radius() + " storage=" + o.storage().toShortString())
                    .orElse("none");
            String body = entity == null ? "unloaded"
                    : McaAdapter.body(entity).map(villager -> "loaded activity=" + McaAdapter.activityName(villager)).orElse("loaded");
            String home = record.settlement().map(Object::toString).orElse("none");
            source.sendSuccess(() -> Component.literal(record.id() + " name=" + record.name() + " settlement=" + home + " order=[" + order
                    + "] delivered=" + record.delivered() + " body=" + body), false);
        }
        return records.size();
    }

    private static Entity findLoaded(CommandSourceStack source, ResidentRecord record) {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(record.entity());
            if (entity != null) return entity;
        }
        return null;
    }

    private static Villager body(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "villager");
        return McaAdapter.body(entity).orElseThrow(NOT_MCA_VILLAGER::create);
    }
}
