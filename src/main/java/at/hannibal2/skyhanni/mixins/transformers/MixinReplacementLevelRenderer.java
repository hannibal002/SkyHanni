package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if < 26.1 {
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
//? } else {
/*import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
*///?}
import com.mojang.blaze3d.textures.GpuSampler;

// Adapted from Fabric API implementation
// The fabric api event makes our lines render strange
@Mixin(LevelRenderer.class)
public class MixinReplacementLevelRenderer {

    @Unique
    PoseStack contextMatrixStack;

    @Unique
    //~ if > 1.21.11 'Camera' -> 'CameraRenderState'
    //~ if > 1.21.11 'currentCamera' -> 'currentCameraState'
    Camera currentCamera;

    @Unique
    DeltaTracker currentTickCounter;

    @Final
    @Shadow
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    //~ if > 1.21.11 'allocator' -> 'resourceAllocator'
    //~ if > 1.21.11 'tickCounter' -> 'deltaTracker'
    //~ if > 1.21.11 'renderBlockOutline' -> 'renderOutline'
    //~ if > 1.21.11 'Camera camera' -> 'CameraRenderState cameraState'
    //~ if > 1.21.11 'Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix' -> 'Matrix4fc modelViewMatrix'
    //~ if > 1.21.11 'fogBuffer' -> 'terrainFog'
    //~ if > 1.21.11 'boolean renderSky' -> 'boolean shouldRenderSky'
    //~ if > 1.21.11 'CallbackInfo ci' -> 'ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci'
    private void beginRender(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        //~ if > 1.21.11 'currentCamera = camera' -> 'currentCameraState = cameraState'
        currentCamera = camera;
        //~ if > 1.21.11 'tickCounter' -> 'deltaTracker'
        currentTickCounter = tickCounter;
    }

    @WrapOperation(
        //~ if > 1.21.11 'method_62214' -> 'lambda$addMainPass$0'
        method = "method_62214",
        slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=translucent")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 0)
    )
    private void onTranslucentRender(ChunkSectionsToRender instance, ChunkSectionLayerGroup group, GpuSampler gpuSampler, Operation<Void> original) {
        original.call(instance, group, gpuSampler);
        if (contextMatrixStack == null) return;

        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            contextMatrixStack,
            //~ if > 1.21.11 'currentCamera' -> 'currentCameraState'
            currentCamera,
            renderBuffers.bufferSource(),
            currentTickCounter.getGameTimeDeltaPartialTick(true),
            true
        );
        event.post();
        contextMatrixStack = null;
    }

    //~ if > 1.21.11 'method_62214' -> 'lambda$addMainPass$0'
    @ModifyExpressionValue(method = "method_62214", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack onCreateMatrixStack(PoseStack matrixStack) {
        contextMatrixStack = matrixStack;
        return matrixStack;
    }
}
