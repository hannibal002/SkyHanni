package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RenderPipeline.class, remap = false)
public abstract class MixinRenderPipeline {

    @ModifyReturnValue(method = "getDepthStencilState", at = @At("RETURN"))
    private DepthStencilState setGlowDepth(DepthStencilState original) {
        RenderPipeline thisPipeline = (RenderPipeline) (Object) this;
        if (thisPipeline != RenderPipelines.OUTLINE_CULL && thisPipeline != RenderPipelines.OUTLINE_NO_CULL) return original;
        if (!SkyHanniOutlineHook.isCurrentlyActive()) return original;
        return original != null
            ? new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, original.writeDepth(), original.depthBiasScaleFactor(), original.depthBiasConstant())
            : new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true);
    }
}
*///?}
