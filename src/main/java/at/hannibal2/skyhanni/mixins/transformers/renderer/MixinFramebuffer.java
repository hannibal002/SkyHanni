package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Framebuffer.class)
public class MixinFramebuffer {

    @ModifyReturnValue(method = "getDepthAttachment", at = @At("RETURN"))
    private GpuTexture modifyDepthAttachment(GpuTexture original) {
        GpuTexture overrideTexture = SkyHanniOutlineVertexConsumerProvider.getOverrideDepthAttachment();
        if (overrideTexture != null) {
            return overrideTexture;
        }
        return original;
    }

}
