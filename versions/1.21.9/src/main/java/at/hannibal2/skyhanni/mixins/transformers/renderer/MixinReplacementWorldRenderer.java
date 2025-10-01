package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Adapted from 1.21.7 fabric api implementation
@Mixin(WorldRenderer.class)
public class MixinReplacementWorldRenderer {

    @Unique
    MatrixStack contextMatrixStack;

    @Unique
    Camera currentCamera;

    @Unique
    RenderTickCounter currentTickCounter;

    @Final
    @Shadow
    private BufferBuilderStorage bufferBuilders;

    @Inject(method = "render", at = @At(value = "HEAD"))
    private void beginRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        currentCamera = camera;
        currentTickCounter = tickCounter;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getCloudRenderModeValue()Lnet/minecraft/client/option/CloudRenderMode;"))
    private void afterTranslucent(CallbackInfo ci) {
        if (contextMatrixStack == null) return;

        SkyHanniRenderWorldEvent event = new SkyHanniRenderWorldEvent(
            contextMatrixStack,
            currentCamera,
            bufferBuilders.getEntityVertexConsumers(),
            currentTickCounter.getTickProgress(true),
            true
        );
        event.post();
        contextMatrixStack = null;
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"))
    private void setMatrixStack(CallbackInfo ci, @Local MatrixStack matrixStack) {
        contextMatrixStack = matrixStack;
    }
}
