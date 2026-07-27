package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.function.Supplier;

//? if < 26.2 {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    //? if >= 26.2 {
    @Inject(
        method = "executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V",
        at = @At("HEAD")
    )
    public void computeChromaBufferSlice(
        Supplier<String> nameSupplier,
        RenderTarget framebuffer,
        GpuBufferSlice dynamicTransformsBuffer,
        int from,
        int _to,
        CallbackInfo ci
    ) {
        GuiRendererHook.INSTANCE.computeChromaBufferSlice();
    }

    @Inject(
        method = "executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            shift = At.Shift.AFTER
        )
    )
    public void insertChromaSetUniform(
        Supplier<String> nameSupplier,
        RenderTarget framebuffer,
        GpuBufferSlice dynamicTransformsBuffer,
        int from,
        int _to,
        CallbackInfo ci,
        @Local RenderPass renderPass
    ) {
        GuiRendererHook.INSTANCE.insertChromaSetUniform(renderPass);
    }
    //?} else {
    /*@Inject(
        method = "executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V",
        at = @At("HEAD")
    )
    public void computeChromaBufferSlice(
        Supplier<String> label,
        RenderTarget mainRenderTarget,
        GpuBufferSlice fogBuffer,
        GpuBufferSlice dynamicTransforms,
        GpuBuffer indexBuffer,
        VertexFormat.IndexType indexType,
        int startIndex,
        int endIndex,
        CallbackInfo ci
    ) {
        GuiRendererHook.INSTANCE.computeChromaBufferSlice();
    }

    @Inject(
        method = "executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    public void insertChromaSetUniform(
        Supplier<String> label,
        RenderTarget mainRenderTarget,
        GpuBufferSlice fogBuffer,
        GpuBufferSlice dynamicTransforms,
        GpuBuffer indexBuffer,
        VertexFormat.IndexType indexType,
        int startIndex,
        int endIndex,
        CallbackInfo ci,
        @Local RenderPass renderPass
    ) {
        GuiRendererHook.INSTANCE.insertChromaSetUniform(renderPass);
    }
    *///?}

    @WrapOperation(
        method = "addElementToMesh",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiElementRenderState;pipeline()Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
    )
    public RenderPipeline replacePipeline(GuiElementRenderState state, Operation<RenderPipeline> original) {
        return GuiRendererHook.INSTANCE.replacePipeline(state, original);
    }

    //? if >= 26.1 {
    @Unique
    private int skyhanni$frameNumber;
    //?} else {
    /*@Shadow
    private int frameNumber;
    *///?}

    //? if >= 26.1 {
    @Inject(method = "render", at = @At("HEAD"))
    private void skyhanni$trackFrameNumber(CallbackInfo ci) {
        skyhanni$frameNumber++;
    }
    //?}

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    //? if < 26.2 {
    /*@Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;
    *///?}

    @Shadow
    @Final
    GuiRenderState renderState;

    @Shadow
    @Final
    private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;

    @Inject(
        method = "preparePictureInPicture",
        at = @At("HEAD")
    )
    private void skyhanni$preRenderAtlas(CallbackInfo ci) {
        GuiRendererHook.INSTANCE.preRenderAtlas(
            pictureInPictureRenderers,
            //? if < 26.2 {
            /*bufferSource,
            *///?}
            featureRenderDispatcher,
            //~ if < 26.1 'skyhanni$frameNumber' -> 'frameNumber'
            skyhanni$frameNumber
        );
    }

    @Inject(
        method = "preparePictureInPictureState",
        at = @At("TAIL")
    )
    private void skyhanni$prepareSkyHanniItems(
        PictureInPictureRenderState state,
        int guiScale,
        CallbackInfo ci
    ) {
        if (!(state instanceof SkyHanniGuiItemRenderState skyHanniState)) return;
        GuiRendererHook.INSTANCE.submitBlitForState(
            skyHanniState,
            renderState,
            //~ if < 26.1 'skyhanni$frameNumber' -> 'frameNumber'
            skyhanni$frameNumber
        );
    }
}
