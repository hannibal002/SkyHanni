package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent;
import at.hannibal2.skyhanni.events.render.OverlayType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class MixinInGameOverlayRenderer {

    //? if >= 26.2 {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void renderFire(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.FIRE).post()) ci.cancel();
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void renderWater(Minecraft client, PoseStack matrices, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.WATER).post()) ci.cancel();
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void renderBlock(TextureAtlasSprite sprite, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int color, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.BLOCK).post()) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void renderBlock(TextureAtlasSprite sprite, PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource bufferSource, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.BLOCK).post()) ci.cancel();
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void renderWater(Minecraft client, PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource bufferSource, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.WATER).post()) ci.cancel();
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void renderFire(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (new BlockOverlayRenderEvent(OverlayType.FIRE).post()) ci.cancel();
    }
    *///?}

}
