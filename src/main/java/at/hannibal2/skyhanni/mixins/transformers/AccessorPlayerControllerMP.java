package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;

@Mixin(MultiPlayerGameMode.class)
public interface AccessorPlayerControllerMP {
    @Accessor("destroyBlockPos")
    BlockPos skyhanni_getCurrentBlock();
}
