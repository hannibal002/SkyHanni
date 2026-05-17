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
/*import net.minecraft.client.Camera;
import org.joml.Matrix4f;
*///? } else {
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
//?}
import com.mojang.blaze3d.textures.GpuSampler;

// Adapted from Fabric API implementation
// The fabric api event makes our lines render strange
@Mixin(LevelRenderer.class)
public class MixinReplacementLevelRenderer {

    @Unique
    PoseStack contextMatrixStack;

    @Unique
    //~ if < 26.1 'CameraRenderState' -> 'Camera'
    //~ if < 26.1 'currentCameraState' -> 'currentCamera'
    CameraRenderState currentCameraState;

    @Unique
    DeltaTracker currentTickCounter;

    @Final
    @Shadow
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    //~ if < 26.1 'resourceAllocator' -> 'allocator'
    //~ if < 26.1 'deltaTracker' -> 'tickCounter'
    //~ if < 26.1 'renderOutline' -> 'renderBlockOutline'
    //~ if < 26.1 'CameraRenderState cameraState' -> 'Camera camera'
    //~ if < 26.1 'Matrix4fc modelViewMatrix' -> 'Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix'
    //~ if < 26.1 'terrainFog' -> 'fogBuffer'
    //~ if < 26.1 'boolean shouldRenderSky' -> 'boolean renderSky'
    //~ if < 26.1 'ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci' -> 'CallbackInfo ci'
    private void beginRender(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        //~ if < 26.1 'currentCameraState = cameraState' -> 'currentCamera = camera'
        currentCameraState = cameraState;
        //~ if < 26.1 'deltaTracker' -> 'tickCounter'
        currentTickCounter = deltaTracker;
    }

    @WrapOperation(
        //~ if < 26.1 'lambda$addMainPass$0' -> 'method_62214'
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

    //~ if < 26.1 'lambda$addMainPass$0' -> 'method_62214'
    @ModifyExpressionValue(method = "lambda$addMainPass$0", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    private PoseStack onCreateMatrixStack(PoseStack matrixStack) {
        contextMatrixStack = matrixStack;
        return matrixStack;
    }
}
