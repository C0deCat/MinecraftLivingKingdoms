package livingkingdoms.settlements.gametest.survey;

import livingkingdoms.settlements.LivingKingdomsSettlements;
import livingkingdoms.settlements.survey.Walkability;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of the walkable-layout measurement on the template {@code platform} (floor at y=1):
 * known layouts with known answers, so the village survey of M4-B can be trusted.
 */
@GameTestHolder(LivingKingdomsSettlements.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WalkabilityTests {
    private static final BlockPos BELL = new BlockPos(4, 2, 4);

    private WalkabilityTests() {}

    @GameTest(template = "platform")
    public static void doorOnTheGroundAndOnASingleStepAreReachable(GameTestHelper helper) {
        helper.setBlock(BELL, Blocks.BELL);
        BlockPos ground = new BlockPos(1, 2, 1);
        placeDoor(helper, ground);
        helper.setBlock(new BlockPos(7, 2, 1), Blocks.STONE);
        BlockPos step = new BlockPos(7, 3, 1);
        placeDoor(helper, step);

        Walkability.Result result = measure(helper);

        helper.assertTrue(result.bell(), "The bell was not found");
        helper.assertTrue(result.doors().size() == 2, "Expected 2 doors, found " + result.doors().size());
        helper.assertTrue(result.reachedDoors() == 2, "Expected both doors reachable, got " + result.reachedDoors());
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void doorOnATwoBlockLedgeIsNotReachable(GameTestHelper helper) {
        // The ledge is in the corner, more than three blocks from the bell: the bell's top can be stood on and
        // would otherwise count as a walkable position near the door.
        helper.setBlock(BELL, Blocks.BELL);
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.STONE);
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.STONE);
        BlockPos ledge = new BlockPos(8, 4, 8);
        placeDoor(helper, ledge);

        Walkability.Result result = measure(helper);

        helper.assertTrue(result.doors().size() == 1, "Expected 1 door, found " + result.doors().size());
        Walkability.Door door = result.doors().get(0);
        helper.assertTrue(!door.reached(), "A door two blocks above the ground must not count as reachable");
        helper.assertTrue(door.gap() == 2, "Expected a gap of 2 blocks to the walkable area, got " + door.gap());
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void centreIsTheStreetAndNotAnIsolatedPerchNextToTheBell(GameTestHelper helper) {
        // A bell on a two-block pillar: the nearest standable positions are on the pillar tops around it.
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 4, 4), Blocks.BELL);
        helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
        placeDoor(helper, new BlockPos(1, 2, 1));

        Walkability.Result result = measure(helper);

        helper.assertTrue(result.reachedDoors() == 1, "The door on the ground must be reachable from the street around the bell: " + result);
        helper.succeed();
    }

    private static Walkability.Result measure(GameTestHelper helper) {
        BlockPos min = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos max = helper.absolutePos(new BlockPos(8, 5, 8));
        return Walkability.measure(helper.getLevel(), BoundingBox.fromCorners(min, max));
    }

    private static void placeDoor(GameTestHelper helper, BlockPos lower) {
        helper.setBlock(lower, Blocks.OAK_DOOR.defaultBlockState());
        helper.setBlock(lower.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
