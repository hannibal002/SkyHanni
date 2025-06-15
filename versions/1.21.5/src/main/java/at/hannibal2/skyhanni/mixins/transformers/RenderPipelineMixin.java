package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
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
        return ((Object) this == RenderPipelines.OUTLINE_CULL || (Object) this == RenderPipelines.OUTLINE_NO_CULL) && (RenderLivingEntityHelper.INSTANCE.getAreMobsHighlighted() && !RenderLivingEntityHelper.INSTANCE.getRenderingRealGlow()) ? DepthTestFunction.LEQUAL_DEPTH_TEST : original;
    }
}
