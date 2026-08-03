package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
//?} else {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
*///?}

//? if >= 26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
//?} else {
/*import net.minecraft.client.Camera;
import org.joml.Matrix4f;
*///?}

// Adapted from Fabric API implementation
// The Fabric API event makes our lines render strange
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    //? if < 26.2 {
    /*@Unique
    PoseStack contextMatrixStack;
    *///?}

    @Unique
    //~ if < 26.1 'CameraRenderState currentCameraState' -> 'Camera currentCamera'
    CameraRenderState currentCameraState;

    @Unique
    DeltaTracker currentTickCounter;

    //? if >= 26.2 {
    @Final
    @Shadow
    private SubmitNodeStorage submitNodeStorage;
    //?} else {
    /*@Final
    @Shadow
    private RenderBuffers renderBuffers;
    *///?}

    //~ if < 26.2 'render' -> 'renderLevel'
    @Inject(method = "render", at = @At("HEAD"))
    private void beginRender(
        GraphicsResourceAllocator resourceAllocator,
        DeltaTracker deltaTracker, boolean renderOutline,
        //~ if < 26.1 'CameraRenderState' -> 'Camera'
        CameraRenderState cameraState,
        //? if >= 26.1 {
        Matrix4fc modelViewMatrix,
        //?} else {
        /*Matrix4f positionMatrix,
        Matrix4f matrix4f,
        Matrix4f projectionMatrix,
        *///?}
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        //? if = 26.1
        //ChunkSectionsToRender chunkSectionsToRender,
        CallbackInfo ci
    ) {
        //~ if < 26.1 'currentCameraState' -> 'currentCamera'
        currentCameraState = cameraState;
        currentTickCounter = deltaTracker;
    }

    //? if >= 26.2 {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;prepareFrame(Lnet/minecraft/client/renderer/SubmitNodeStorage;)Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"
        )
    )
    private void postRenderWorldBeforePrepareFeatures(
        GraphicsResourceAllocator resourceAllocator,
        DeltaTracker deltaTracker,
        boolean renderOutline,
        CameraRenderState cameraState,
        Matrix4fc modelViewMatrix,
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        CallbackInfo ci
    ) {
        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            new PoseStack(),
            cameraState,
            submitNodeStorage,
            deltaTracker.getGameTimeDeltaPartialTick(true),
            true
        );
        event.post();
    }
    //?} else {
    /*@WrapOperation(
        method = "lambda$addMainPass$0",
        slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=translucent")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 0)
    )
    private void onTranslucentRender(ChunkSectionsToRender instance, ChunkSectionLayerGroup group, GpuSampler gpuSampler, Operation<Void> original) {
        original.call(instance, group, gpuSampler);
        if (contextMatrixStack == null) return;

        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            contextMatrixStack,
            //~ if < 26.1 'currentCameraState' -> 'currentCamera'
            currentCameraState,
            renderBuffers.bufferSource(),
            currentTickCounter.getGameTimeDeltaPartialTick(true),
            true
        );
        event.post();
        contextMatrixStack = null;
    }

    @ModifyExpressionValue(method = "lambda$addMainPass$0", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack onCreateMatrixStack(PoseStack matrixStack) {
        contextMatrixStack = matrixStack;
        return matrixStack;
    }

    @Inject(method = "extractVisibleEntities", at = @At(value = "HEAD"))
    public void resetRealGlowing(CallbackInfo ci) {
        RenderLivingEntityHelper.postNoXrayOutlineEvent();
    }

    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;appearsGlowing()Z"))
    public boolean shouldAlsoGlow(EntityRenderState instance, Operation<Boolean> original, @Local Entity entity) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        return glowColor != null || original.call(instance);
    }
    *///?}

    //~ if < 26.2 ';Lorg/joml/Vector4fc;' -> ';I'
    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setGlowDepth(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.isUsingCustomGlow()) return;
        SkyHanniOutlineVertexConsumerProvider.checkIfDepthAttachmentNeedsUpdating();
    }

    //? if < 26.2 {
    /*@Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void renderSkyhanniGlow(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.isUsingCustomGlow()) return;
        SkyHanniOutlineVertexConsumerProvider.getVertexConsumers().endOutlineBatch();
    }
    *///?}

    //? if = 26.1 {
    /*@Inject(
        method = "lambda$addLateDebugPass$0",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;outputDepthTextureOverride:Lcom/mojang/blaze3d/textures/GpuTextureView;",
            shift = At.Shift.AFTER
        )
    )
    private void renderDeferredSeeThroughText(CallbackInfo ci, @Local MultiBufferSource.BufferSource bufferSource) {
        WorldRenderUtils.renderDeferredSeeThroughText(bufferSource);
    }
    *///?}
}
