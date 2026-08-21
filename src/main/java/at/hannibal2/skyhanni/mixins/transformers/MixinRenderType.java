package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderType.class)
public abstract class MixinRenderType {

    @ModifyArg(
        method = "prepare",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/rendertype/OutputTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/systems/ScissorState;Ljava/util/List;)V"
        ),
        index = 0
    )
    private RenderPipeline useSkyHanniCustomDepthOutlinePipeline(RenderPipeline pipeline) {
        return SkyHanniOutlineHook.getCustomOutlinePipelineForBuild(pipeline);
    }
}
//?}
