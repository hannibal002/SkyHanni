package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.fishing.LavaReplacement;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BlockFluidRenderer.class)
public abstract class MixinBlockLiquid {

    @Shadow
    private TextureAtlasSprite[] atlasSpritesWater;

    @ModifyVariable(
        method = "renderFluid",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockLiquid;colorMultiplier(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/BlockPos;)I")
    )
    TextureAtlasSprite[] replaceRenderedFluid(TextureAtlasSprite[] value) {
        if (LavaReplacement.replaceLava()) {
            return this.atlasSpritesWater;
        }
        return value;
    }
}
