package livingkingdoms.settlements.gametest.settlement;

import java.util.List;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import livingkingdoms.settlements.mca.McaAdapter;
import livingkingdoms.settlements.resident.Residents;
import livingkingdoms.settlements.settlement.SettlementRecord;
import livingkingdoms.settlements.settlement.Settlements;
import livingkingdoms.settlements.settlement.SiteSafety;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** GameTests of settlements on the template {@code platform} (floor at y=1). */
@GameTestHolder(LivingKingdomsSettlements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SettlementTests {
    private SettlementTests() {}

    @GameTest(template = "platform")
    public static void villagerAppearingInsideASettlementBecomesItsResident(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        // The settlement covers the middle of the plot only, so villagers of neighbouring tests stay outside it.
        BoundingBox bounds = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(3, 1, 3)), helper.absolutePos(new BlockPos(5, 5, 5)));
        long startChunk = new ChunkPos(helper.absolutePos(new BlockPos(4, 1, 4))).toLong();
        SettlementRecord settlement = Settlements.found(server, "test:village", startChunk, bounds);

        Villager inside = McaAdapter.spawnAdult(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 2.0, 4.5)));
        Villager outside = McaAdapter.spawnAdult(helper.getLevel(), helper.absoluteVec(new Vec3(0.5, 2.0, 0.5)));

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Residents.settlementOf(inside).filter(settlement.id()::equals).isPresent(),
                    "The villager inside the settlement is not its resident");
            helper.assertTrue(!Residents.isResident(outside), "A villager outside every settlement became a resident");
            helper.assertTrue(Residents.countIn(server, settlement.id()) == 1, "The settlement must count one resident");
            helper.assertTrue(!Residents.hasWork(inside), "A new resident has no work yet and keeps the brain MCA builds");
            Residents.dismiss(inside);
            Settlements.remove(server, settlement.id());
            helper.succeed();
        });
    }

    @GameTest(template = "platform")
    public static void openLavaNextToABuildingHardensAndAForgeInsideItStays(GameTestHelper helper) {
        BoundingBox plot = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(8, 5, 8)));
        BoundingBox building = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 1, 1)), helper.absolutePos(new BlockPos(3, 4, 3)));
        BlockPos forge = new BlockPos(2, 1, 2);
        BlockPos lake = new BlockPos(5, 1, 2);
        BlockPos farLake = new BlockPos(8, 1, 8);
        BlockPos fire = new BlockPos(4, 2, 2);
        helper.setBlock(forge, Blocks.LAVA);
        helper.setBlock(lake, Blocks.LAVA);
        helper.setBlock(farLake, Blocks.LAVA);
        helper.setBlock(fire, Blocks.FIRE);

        // A lake that reaches into the box of a second building is still a lake.
        BoundingBox farm = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(1, 1, 5)), helper.absolutePos(new BlockPos(3, 4, 8)));
        for (int x = 1; x <= 3; x++) {
            for (int z = 6; z <= 7; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.LAVA);
        }

        int changed = SiteSafety.clear(helper.getLevel(), List.of(building, farm), plot);

        helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(2, 1, 6));
        helper.assertTrue(changed == 8, "Expected the lake block, the fire and the six blocks of the lake under the farm to change, changed " + changed);
        helper.assertBlockPresent(Blocks.OBSIDIAN, lake);
        helper.assertBlockPresent(Blocks.AIR, fire);
        helper.assertBlockPresent(Blocks.LAVA, forge);
        helper.assertBlockPresent(Blocks.LAVA, farLake);
        // Keep the plot from burning while other tests run.
        helper.setBlock(forge, Blocks.GRASS_BLOCK);
        helper.setBlock(farLake, Blocks.GRASS_BLOCK);
        helper.succeed();
    }
}
