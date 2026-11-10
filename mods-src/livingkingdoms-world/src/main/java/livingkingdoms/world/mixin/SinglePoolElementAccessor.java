package livingkingdoms.world.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the processor list of a single-template pool element, so the structure scan knows which
 * pieces our {@code foundation} processor grounds (Lithostitched {@code set_pool_element_processors}
 * replaces this field with the old list plus ours).
 */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
    @Accessor("processors")
    Holder<StructureProcessorList> livingkingdoms$processors();
}
