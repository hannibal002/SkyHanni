package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.features.misc.LavaReplacementConfig;
import io.github.moulberry.notenoughupdates.util.SpecialColour;
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
    private TextureAtlasSprite[] injected(TextureAtlasSprite[] textureAtlasSprites) {
        return (SkyHanniMod.Companion.getFeature().misc.lavaReplacementConfig.renderType.get() == LavaReplacementConfig.RenderType.TEXTURE)
            ? atlasSpritesWater : textureAtlasSprites;
    }

    @ModifyVariable(method = "renderFluid", at = @At("STORE"))
    private int injected(int i) {
        return (SkyHanniMod.Companion.getFeature().misc.lavaReplacementConfig.renderType.get() == LavaReplacementConfig.RenderType.COLOR)
                ? SpecialColour.specialToChromaRGB(SkyHanniMod.Companion.getFeature().misc.lavaReplacementConfig.color) : i;
    }
}
