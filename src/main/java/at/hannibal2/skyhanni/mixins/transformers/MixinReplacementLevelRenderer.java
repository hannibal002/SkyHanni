//? if >= 26.2 {
package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
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

//?if >= 26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
//?} else {
/*import net.minecraft.client.Camera;
import org.joml.Matrix4f;
*///?}

//? if >= 26.2
import net.minecraft.client.renderer.SubmitNodeStorage;

// Adapted from Fabric API implementation
// The Fabric API event makes our lines render strange
@Mixin(LevelRenderer.class)
public abstract class MixinReplacementLevelRenderer {

    @Unique
    //~ if < 26.1 'CameraRenderState currentCameraState' -> 'Camera currentCamera'
    CameraRenderState currentCameraState;

    @Unique
    DeltaTracker currentTickCounter;

    @Final
    @Shadow
    private RenderBuffers renderBuffers;

    //? if >= 26.2 {
    @Final
    @Shadow
    private SubmitNodeStorage submitNodeStorage;
    //?}

    //~ if < 26.2 '"render"' -> '"renderLevel"'
    @Inject(method = "render", at = @At(value = "HEAD"))
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
        CallbackInfo ci
    ) {
        //~ if < 26.1 'currentCameraState' -> 'currentCamera'
        currentCameraState = cameraState;
        currentTickCounter = deltaTracker;
    }

    @WrapOperation(
        method = "lambda$addMainPass$0",
        slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=translucent")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 0)
    )
    private void onTranslucentRender(ChunkSectionsToRender instance, ChunkSectionLayerGroup group, GpuSampler gpuSampler, Operation<Void> original) {
        original.call(instance, group, gpuSampler);

        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            new PoseStack(),
            //~ if < 26.1 'currentCameraState' -> 'currentCamera'
            currentCameraState,
            //? if >= 26.2
            submitNodeStorage,
            //? if < 26.2
            //renderBuffers.bufferSource(),
            currentTickCounter.getGameTimeDeltaPartialTick(true),
            true
        );
        event.post();
    }
}
//?}
