package livingkingdoms.settlements.gametest.work;

import java.util.Optional;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import livingkingdoms.settlements.mca.McaAdapter;
import livingkingdoms.settlements.resident.ResidentRecord;
import livingkingdoms.settlements.resident.Residents;
import livingkingdoms.settlements.work.HarvestOrder;
import livingkingdoms.settlements.work.Labor;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of the harvest order (D-51: settlement GameTests live in this mod). Run with
 * {@code ./gradlew :livingkingdoms-settlements:runGameTestServer}; MCA Reborn is on the dev
 * classpath. The template {@code platform} is a 9x5x9 box with a grass floor in its lowest layer.
 */
@GameTestHolder(LivingKingdomsSettlements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HarvestOrderTests {
    // Test coordinates start at the structure block, one below the template: the floor is y=1.
    private static final BlockPos[] FIELD = {new BlockPos(2, 2, 2), new BlockPos(3, 2, 2), new BlockPos(4, 2, 2)};
    private static final BlockPos FIELD_CENTER = new BlockPos(3, 2, 2);
    private static final BlockPos CHEST = new BlockPos(6, 2, 6);
    private static final Vec3 START = new Vec3(1.5, 2.0, 6.5);

    private HarvestOrderTests() {}

    @GameTest(template = "platform", timeoutTicks = 1200)
    public static void residentHarvestsWheatAndDeliversItToChest(GameTestHelper helper) {
        plantRipeWheat(helper);
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setDayTime(Labor.START + 1000);

        Villager villager = McaAdapter.spawnAdult(helper.getLevel(), helper.absoluteVec(START));
        Residents.setOrder(villager, Optional.of(new HarvestOrder(helper.absolutePos(FIELD_CENTER), 2, helper.absolutePos(CHEST))));
        McaAdapter.refreshBrain(villager);

        helper.succeedWhen(() -> {
            int wheat = helper.<ChestBlockEntity>getBlockEntity(CHEST).countItem(Items.WHEAT);
            helper.assertTrue(wheat == FIELD.length, "The chest holds " + wheat + " wheat, expected " + FIELD.length);
            for (BlockPos pos : FIELD) {
                helper.assertBlockState(pos, HarvestOrderTests::isReplantedWheat, () -> "The crop was not replanted");
            }
            int delivered = Residents.recordOf(villager).map(ResidentRecord::delivered).orElse(0);
            helper.assertTrue(delivered >= FIELD.length, "The record counts " + delivered + " delivered items");
        });
    }

    @GameTest(template = "platform", timeoutTicks = 500)
    public static void villagerWithoutOrderLeavesTheFieldAlone(GameTestHelper helper) {
        plantRipeWheat(helper);
        helper.setBlock(CHEST, Blocks.CHEST);
        helper.setDayTime(Labor.START + 1000);

        Villager villager = McaAdapter.spawnAdult(helper.getLevel(), helper.absoluteVec(START));

        helper.runAfterDelay(400, () -> {
            helper.assertTrue(!Residents.isResident(villager), "A spawned MCA villager must not be a resident");
            for (BlockPos pos : FIELD) {
                helper.assertBlockState(pos, state -> state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state),
                        () -> "A villager without an order touched the field");
            }
            helper.succeed();
        });
    }

    private static void plantRipeWheat(GameTestHelper helper) {
        for (BlockPos pos : FIELD) {
            helper.setBlock(pos.below(), Blocks.FARMLAND);
            helper.setBlock(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(CropBlock.MAX_AGE));
        }
    }

    private static boolean isReplantedWheat(BlockState state) {
        return state.is(Blocks.WHEAT) && !((CropBlock) Blocks.WHEAT).isMaxAge(state);
    }
}
