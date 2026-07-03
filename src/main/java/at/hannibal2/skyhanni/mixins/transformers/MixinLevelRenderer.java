package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineHook;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
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
/*import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
*///?}

// Adapted from Fabric API implementation
// The Fabric API event makes our lines render strange
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    //? if < 26.2 {
    /*@Final
    @Shadow
    private RenderBuffers renderBuffers;
    *///?}

    //? if >= 26.2 {
    @Final
    @Shadow
    private SubmitNodeStorage submitNodeStorage;
    //?}

    @Unique
    CameraRenderState skyhanni$currentCameraState;

    @Unique
    DeltaTracker skyhanni$currentDeltaTracker;

    //~ if < 26.2 'render' -> 'renderLevel'
    @Inject(method = "render", at = @At("HEAD"))
    private void beginRender(
        GraphicsResourceAllocator resourceAllocator,
        DeltaTracker deltaTracker,
        boolean renderOutline,
        CameraRenderState cameraState,
        Matrix4fc modelViewMatrix,
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        //? if < 26.2
        //ChunkSectionsToRender chunkSectionsToRender,
        CallbackInfo ci
    ) {
        skyhanni$currentCameraState = cameraState;
        skyhanni$currentDeltaTracker = deltaTracker;
    }

    @WrapOperation(
        method = "lambda$addMainPass$0",
        slice = @Slice(
            from = @At(
                value = "INVOKE_STRING",
                target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
                args = "ldc=translucentTerrain"
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
            ordinal = 0
        )
    )
    private void onTranslucentRender(
        ChunkSectionsToRender instance,
        ChunkSectionLayerGroup group,
        GpuSampler sampler,
        Operation<Void> original
    ) {
        original.call(instance, group, sampler);

        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            new PoseStack(),
            skyhanni$currentCameraState,
            //? if >= 26.2
            submitNodeStorage,
            //? else
            //renderBuffers.bufferSource(),
            skyhanni$currentDeltaTracker.getGameTimeDeltaPartialTick(true),
            true
        );
        event.post();
    }

    @Inject(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            //? if >= 26.2
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
            //? else
            //target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void setGlowDepth(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.getAreMobsHighlighted()) return;
        SkyHanniOutlineHook.checkIfDepthAttachmentNeedsUpdating();
    }

    //? if < 26.2 {
    /*@Inject(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"
        )
    )
    private void renderSkyHanniGlow(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.getAreMobsHighlighted()) return;
        SkyHanniOutlineHook.getVertexConsumers().endOutlineBatch();
    }
    *///?}
}
