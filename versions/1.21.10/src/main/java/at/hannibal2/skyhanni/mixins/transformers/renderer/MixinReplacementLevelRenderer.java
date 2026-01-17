package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from 1.21.7 and 1.21.10 fabric api implementation
@Mixin(LevelRenderer.class)
public class MixinReplacementLevelRenderer {

    @Unique
    PoseStack contextMatrixStack;

    @Unique
    Camera currentCamera;

    @Unique
    DeltaTracker currentTickCounter;

    @Final
    @Shadow
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    private void beginRender(GraphicsResourceAllocator allocator, DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        currentCamera = camera;
        currentTickCounter = tickCounter;
    }

    // TODO(Ravel): @At.args is not supported
// TODO(Ravel): @At.args is not supported
    @WrapOperation(method = "method_62214",
        slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=translucent")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;)V", ordinal = 0)
    )
    private void onTranslucentRender(ChunkSectionsToRender instance, ChunkSectionLayerGroup group, Operation<Void> original) {
        original.call(instance, group);
        if (contextMatrixStack == null) return;

        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            contextMatrixStack,
            currentCamera,
            renderBuffers.bufferSource(),
            currentTickCounter.getGameTimeDeltaPartialTick(true),
            true
        );
        event.post();
        contextMatrixStack = null;
    }

    @ModifyExpressionValue(method = "method_62214", at = @At(value = "NEW", target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
        private PoseStack onCreateMatrixStack(PoseStack matrixStack) {
        contextMatrixStack = matrixStack;
        return matrixStack;
    }
}
