package livingkingdoms.world.mixin;

import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import livingkingdoms.world.FixedCountPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets vanilla structure search ({@code /locate}, eyes of ender, explorer maps) find structures placed
 * by our {@code livingkingdoms_world:fixed_count} placement. Vanilla {@code findNearestMapStructure}
 * only knows concentric rings and random spread and skips any other placement type, so without this
 * an eye of ender would never lead to a stronghold of our set. Our positions are checked with the same
 * vanilla test as stronghold rings ({@code getStructureGeneratingAt}); the nearer result wins.
 */
@Mixin(ChunkGenerator.class)
abstract class ChunkGeneratorMixin {
    @Shadow
    private static Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(Set<Holder<Structure>> structureHoldersSet, LevelReader level,
                                                                          StructureManager structureManager, boolean skipKnownStructures,
                                                                          StructurePlacement placement, ChunkPos chunkPos) {
        throw new AssertionError();
    }

    @Inject(method = "findNearestMapStructure", at = @At("RETURN"), cancellable = true)
    private void livingkingdoms$findFixedCount(ServerLevel level, HolderSet<Structure> structure, BlockPos pos, int searchRadius,
                                               boolean skipKnownStructures, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        Map<FixedCountPlacement, Set<Holder<Structure>>> ours = new LinkedHashMap<>();
        for (Holder<Structure> holder : structure) {
            for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
                if (placement instanceof FixedCountPlacement fixed) ours.computeIfAbsent(fixed, p -> new LinkedHashSet<>()).add(holder);
            }
        }
        if (ours.isEmpty()) return;

        Pair<BlockPos, Holder<Structure>> best = cir.getReturnValue();
        double bestDistance = best == null ? Double.MAX_VALUE : pos.distSqr(best.getFirst());
        for (Map.Entry<FixedCountPlacement, Set<Holder<Structure>>> entry : ours.entrySet()) {
            Comparator<ChunkPos> nearest = Comparator.comparingDouble(chunk -> chunk.getMiddleBlockPosition(pos.getY()).distSqr(pos));
            for (ChunkPos chunk : entry.getKey().positionsFor(state).stream().sorted(nearest).toList()) {
                if (chunk.getMiddleBlockPosition(pos.getY()).distSqr(pos) >= bestDistance) break;
                Pair<BlockPos, Holder<Structure>> found = getStructureGeneratingAt(entry.getValue(), level, level.structureManager(),
                        skipKnownStructures, entry.getKey(), chunk);
                if (found != null) {
                    best = found;
                    bestDistance = pos.distSqr(found.getFirst());
                    break;
                }
            }
        }
        cir.setReturnValue(best);
    }
}
