package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.BlockFluidRendererHookKt;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockFluidRenderer.class, priority = 1001)
public abstract class MixinBlockLiquid {

    @Shadow
    private TextureAtlasSprite[] atlasSpritesWater;

    @Shadow
    private TextureAtlasSprite[] atlasSpritesLava;

    @Redirect(
        method = "renderFluid",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/BlockFluidRenderer;atlasSpritesLava:[Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
            opcode = 180
        )
    )
    private TextureAtlasSprite[] redirectLavaSprites(BlockFluidRenderer instance) {
        return BlockFluidRendererHookKt.replaceSprite(this.atlasSpritesLava, this.atlasSpritesWater);
    }
}
