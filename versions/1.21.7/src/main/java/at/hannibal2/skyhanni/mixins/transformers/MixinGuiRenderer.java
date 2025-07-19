package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Supplier;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {

    @Inject(method = "render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", at = @At("HEAD"))
    public void computeChromaBufferSlice(
        Supplier<String> nameSupplier,
        Framebuffer framebuffer,
        GpuBufferSlice fogBuffer,
        GpuBufferSlice dynamicTransformsBuffer,
        GpuBuffer buffer,
        VertexFormat.IndexType indexType,
        int from, int _to, CallbackInfo ci) {
        GuiRendererHook.INSTANCE.computeChromaBufferSlice();
    }

    @Inject(method = "render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", ordinal = 1))
    public void insertChromaSetUniform(
        Supplier<String> nameSupplier,
        Framebuffer framebuffer,
        GpuBufferSlice fogBuffer,
        GpuBufferSlice dynamicTransformsBuffer,
        GpuBuffer buffer,
        VertexFormat.IndexType indexType,
        int from, int _to, CallbackInfo ci,
        @Local RenderPass renderPass) {
        GuiRendererHook.INSTANCE.insertChromaSetUniform(renderPass);
    }

    @WrapOperation(method = "prepareSimpleElement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/SimpleGuiElementRenderState;pipeline()Lcom/mojang/blaze3d/pipeline/RenderPipeline;"))
    public RenderPipeline replacePipeline(SimpleGuiElementRenderState state, Operation<RenderPipeline> original) {
        return GuiRendererHook.INSTANCE.replacePipeline(state, original);
    }

}
