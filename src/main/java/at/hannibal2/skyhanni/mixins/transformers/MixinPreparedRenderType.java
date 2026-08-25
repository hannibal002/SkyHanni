package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PreparedRenderType.class)
public abstract class MixinPreparedRenderType {

    @ModifyExpressionValue(
        method = "drawFromBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/IndexType;III)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/rendertype/PreparedRenderType;pipeline:Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        )
    )
    private RenderPipeline useSkyHanniCustomDepthOutlinePipeline(RenderPipeline pipeline) {
        return SkyHanniOutlineHook.getCustomOutlinePipeline(pipeline);
    }

    @Inject(
        method = "drawFromBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/IndexType;III)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            shift = At.Shift.AFTER
        )
    )
    private void bindSkyHanniChromaUniform(
        GpuBuffer vertexBuffer,
        GpuBuffer indexBuffer,
        IndexType indexType,
        int baseVertex,
        int firstIndex,
        int indexCount,
        CallbackInfo ci,
        @Local RenderPass renderPass
    ) {
        PreparedRenderType renderType = (PreparedRenderType) (Object) this;
        GuiRendererHook.INSTANCE.insertChromaSetUniform(renderPass, renderType.pipeline());
    }
}
//?}
