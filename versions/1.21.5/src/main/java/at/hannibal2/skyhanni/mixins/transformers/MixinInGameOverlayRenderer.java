package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent;
import at.hannibal2.skyhanni.events.render.OverlayType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
abstract class MixinInGameOverlayRenderer {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderFire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.FIRE).post()) ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderWater(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.WATER).post()) ci.cancel();
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void renderBlock(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.BLOCK).post()) ci.cancel();
    }

}
