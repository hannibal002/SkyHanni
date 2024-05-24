package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.BlockFluidRendererHookKt;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockFluidRenderer.class)
public class MixinBlockFluidRenderer {

    @Shadow
    private TextureAtlasSprite[] atlasSpritesWater;

    @ModifyVariable(method = "renderFluid", at = @At("STORE"))
    private TextureAtlasSprite[] modifySprite(TextureAtlasSprite[] textureAtlasSprites) {
        return BlockFluidRendererHookKt.modifySprite(textureAtlasSprites, atlasSpritesWater);
    }

    @ModifyVariable(method = "renderFluid", at = @At("STORE"))
    private int modifyColorMultiplier(int i) {
        return BlockFluidRendererHookKt.modifyColorMultiplier(i);
    }
}
