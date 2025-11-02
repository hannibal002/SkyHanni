package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RenderPipeline.class, remap = false)
public class RenderPipelineMixin {

    @ModifyReturnValue(method = "getDepthTestFunction", at = @At("RETURN"))
    private DepthTestFunction setGlowDepth(DepthTestFunction original) {
        RenderPipeline thisPipeline = (RenderPipeline) (Object) this;
        if (thisPipeline != RenderPipelines.OUTLINE_CULL && thisPipeline != RenderPipelines.OUTLINE_NO_CULL) return original;
        return SkyHanniOutlineVertexConsumerProvider.getCurrentlyActive() ? DepthTestFunction.LEQUAL_DEPTH_TEST : original;
    }
}
