package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.BlockFluidRendererHook;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.BlockFluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockFluidRenderer.class)
public abstract class MixinBlockLiquid {

    @Redirect(method = "renderFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockLiquid;getMaterial()Lnet/minecraft/block/material/Material;", ordinal = 0))
    private Material isMaterialWater(BlockLiquid blockLiquid) {
        return BlockFluidRendererHook.replaceLava(blockLiquid);
    }
}
