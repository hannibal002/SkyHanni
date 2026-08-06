package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {

    @Inject(method = "executeDrawRange", at = @At("HEAD"))
    public void computeChromaBufferSlice(
        CallbackInfo ci) {
        GuiRendererHook.INSTANCE.computeChromaBufferSlice();
    }

    @Inject(method = "executeDrawRange", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", ordinal = 1))
    public void insertChromaSetUniform(
        CallbackInfo ci,
        @Local RenderPass renderPass) {
        GuiRendererHook.INSTANCE.insertChromaSetUniform(renderPass);
    }

    @WrapOperation(
        method = "addElementToMesh",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiElementRenderState;pipeline()Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
    )
    public RenderPipeline replacePipeline(GuiElementRenderState state, Operation<RenderPipeline> original) {
        return GuiRendererHook.INSTANCE.replacePipeline(state, original);
    }

    @Unique
    private int skyhanni$frameNumber;

    @Inject(method = "render", at = @At("HEAD"))
    private void skyhanni$trackFrameNumber(CallbackInfo ci) {
        skyhanni$frameNumber++;
    }

    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Unique
    public MultiBufferSource.BufferSource getBufferSource() {
        return bufferSource;
    }

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

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
            getBufferSource(),
            featureRenderDispatcher,
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
            skyhanni$frameNumber
        );
    }
}
