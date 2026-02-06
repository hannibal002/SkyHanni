package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent;
import at.hannibal2.skyhanni.events.render.OverlayType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class MixinInGameOverlayRenderer {

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void renderFire(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.FIRE).post()) ci.cancel();
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void renderWater(Minecraft client, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.WATER).post()) ci.cancel();
    }

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void renderBlock(TextureAtlasSprite sprite, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.BLOCK).post()) ci.cancel();
    }

}
