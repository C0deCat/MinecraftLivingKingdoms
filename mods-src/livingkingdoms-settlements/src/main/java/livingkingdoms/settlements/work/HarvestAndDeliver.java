package livingkingdoms.settlements.work;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import livingkingdoms.settlements.resident.ResidentRecord;
import livingkingdoms.settlements.resident.Residents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Carries out a {@link HarvestOrder}: walk to a ripe crop, harvest and replant it, and when the
 * field has no ripe crop left (or the batch is full) carry the harvest to the storage.
 *
 * <p>One run is one trip to one block. Nothing is kept between runs: the next trip is chosen from
 * the order in the resident's record, the world and the inventory, so the work continues after a
 * world reload without saved task state. Crops and harvest are recognised by tags, not item IDs.
 */
public final class HarvestAndDeliver extends Behavior<Villager> {
    private static final int MAX_TRIP_TICKS = 20 * 60;
    private static final int SEARCH_INTERVAL = 40;
    private static final int WALK_REISSUE_INTERVAL = 20;
    private static final int BATCH = 16;
    private static final float SPEED = 0.5F;
    private static final double REACH = 2.6;

    private BlockPos target;
    private boolean delivering;
    private long nextSearch;
    private long nextWalkIssue;

    public HarvestAndDeliver() {
        super(ImmutableMap.of(), MAX_TRIP_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (level.getGameTime() < nextSearch) return false;
        nextSearch = level.getGameTime() + SEARCH_INTERVAL;
        Optional<HarvestOrder> order = Residents.recordOf(villager).flatMap(ResidentRecord::order);
        if (order.isEmpty()) return false;

        int carried = countHarvest(villager.getInventory());
        Optional<BlockPos> crop = carried >= BATCH ? Optional.empty() : nearestRipeCrop(level, villager, order.get());
        if (crop.isPresent()) {
            target = crop.get();
            delivering = false;
        } else if (carried > 0) {
            target = order.get().storage();
            delivering = true;
        } else {
            target = null;
        }
        return target != null;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        walkToTarget(villager, gameTime);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        return target != null;
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (target.closerToCenterThan(villager.position(), REACH)) {
            if (delivering) deliver(level, villager, target);
            else harvest(level, villager, target);
            target = null;
        } else if (gameTime >= nextWalkIssue && !villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            walkToTarget(villager, gameTime);
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        target = null;
        nextSearch = gameTime + 10;
    }

    private void walkToTarget(Villager villager, long gameTime) {
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, SPEED, 1));
        nextWalkIssue = gameTime + WALK_REISSUE_INTERVAL;
    }

    private static Optional<BlockPos> nearestRipeCrop(ServerLevel level, Villager villager, HarvestOrder order) {
        BlockPos field = order.field();
        int radius = order.radius();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(field.offset(-radius, -1, -radius), field.offset(radius, 1, radius))) {
            if (!isRipeCrop(level.getBlockState(pos))) continue;
            double distance = pos.distToCenterSqr(villager.position());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean isRipeCrop(BlockState state) {
        return state.is(BlockTags.CROPS) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static boolean isHarvest(ItemStack stack) {
        return stack.is(Tags.Items.CROPS) || stack.is(Tags.Items.SEEDS);
    }

    private static int countHarvest(SimpleContainer inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isHarvest(stack)) count += stack.getCount();
        }
        return count;
    }

    private static void harvest(ServerLevel level, Villager villager, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isRipeCrop(state)) return;
        CropBlock crop = (CropBlock) state.getBlock();

        List<ItemStack> drops = Block.getDrops(state, level, pos, null, villager, ItemStack.EMPTY);
        ItemStack seed = crop.getCloneItemStack(level, pos, state);
        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (!replanted && ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                replanted = true;
            }
        }
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
        level.setBlock(pos, replanted ? crop.getStateForAge(0) : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        villager.swing(InteractionHand.MAIN_HAND);

        SimpleContainer inventory = villager.getInventory();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            ItemStack rest = inventory.addItem(drop);
            if (!rest.isEmpty()) Block.popResource(level, pos, rest);
        }
    }

    private void deliver(ServerLevel level, Villager villager, BlockPos storage) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, storage, null);
        if (handler == null) {
            // No container there (broken or never placed): look again later instead of every 2 seconds.
            nextSearch = level.getGameTime() + 200;
            return;
        }
        SimpleContainer inventory = villager.getInventory();
        int delivered = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isHarvest(stack)) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            delivered += stack.getCount() - rest.getCount();
            inventory.setItem(slot, rest);
        }
        villager.swing(InteractionHand.MAIN_HAND);
        if (delivered > 0) Residents.recordDelivery(villager, delivered);
    }
}
