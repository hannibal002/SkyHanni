package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTarget.class)
public class MixinFramebuffer {

    @ModifyReturnValue(method = "getDepthTexture", at = @At("RETURN"))
    private GpuTexture modifyDepthAttachment(GpuTexture original) {
        GpuTexture overrideTexture = SkyHanniOutlineVertexConsumerProvider.getOverrideDepthAttachment();
        if (overrideTexture != null) {
            return overrideTexture;
        }
        return original;
    }

}
