package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.1 {
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
//?} else {
/*import com.mojang.blaze3d.platform.DepthTestFunction;
*///?}

@Mixin(value = RenderPipeline.class, remap = false)
public class RenderPipelineMixin {

    //? if >= 26.1 {
    @ModifyReturnValue(method = "getDepthStencilState", at = @At("RETURN"))
    private DepthStencilState setGlowDepth(DepthStencilState original) {
        RenderPipeline thisPipeline = (RenderPipeline) (Object) this;
        if (thisPipeline != RenderPipelines.OUTLINE_CULL && thisPipeline != RenderPipelines.OUTLINE_NO_CULL) return original;
        if (!SkyHanniOutlineVertexConsumerProvider.getCurrentlyActive()) return original;
        return original != null
            ? new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, original.writeDepth(), original.depthBiasScaleFactor(), original.depthBiasConstant())
            : new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);
    }
    //?} else {
    /*@ModifyReturnValue(method = "getDepthTestFunction", at = @At("RETURN"))
    private DepthTestFunction setGlowDepth(DepthTestFunction original) {
        RenderPipeline thisPipeline = (RenderPipeline) (Object) this;
        if (thisPipeline != RenderPipelines.OUTLINE_CULL && thisPipeline != RenderPipelines.OUTLINE_NO_CULL) return original;
        return SkyHanniOutlineVertexConsumerProvider.getCurrentlyActive() ? DepthTestFunction.LEQUAL_DEPTH_TEST : original;
    }
    *///?}
}
