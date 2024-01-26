package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Block.class)
public interface BlockAccessor {

    @Accessor("maxY")
    void setMaxY_skyhanni(double maxY);
}
