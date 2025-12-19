package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.network.ClientPlayerInteractionManager;

@Mixin(ClientPlayerInteractionManager.class)
public interface AccessorPlayerControllerMP {
    @Accessor("currentBreakingPos")
    BlockPos skyhanni_getCurrentBlock();
}
