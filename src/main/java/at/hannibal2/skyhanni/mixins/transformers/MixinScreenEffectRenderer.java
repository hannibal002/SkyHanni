package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent;
import at.hannibal2.skyhanni.events.render.OverlayType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
*///?}

@Mixin(ScreenEffectRenderer.class)
abstract class MixinScreenEffectRenderer {

    //~ if < 26.2 'submitFire' -> 'renderFire'
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void renderFire(
        PoseStack poseStack,
        //? if >= 26.2 {
        SubmitNodeCollector submitNodeCollector,
        //?} else {
        /*MultiBufferSource bufferSource,
        *///?}
        TextureAtlasSprite sprite,
        CallbackInfo ci

    ) {
        if (new BlockOverlayRenderEvent(OverlayType.FIRE).post()) ci.cancel();
    }

    //~ if < 26.2 'submitWater' -> 'renderWater'
    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void renderWater(
        Minecraft client,
        PoseStack matrices,
        //? if >= 26.2 {
        SubmitNodeCollector submitNodeCollector,
        //?} else {
        /*MultiBufferSource bufferSource,
        *///?}
        CallbackInfo ci
    ) {
        if (new BlockOverlayRenderEvent(OverlayType.WATER).post()) ci.cancel();
    }

    //~ if < 26.2 'submitBlockSprite' -> 'renderTex'
    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void renderBlock(
        TextureAtlasSprite sprite,
        PoseStack poseStack,
        //? if >= 26.2 {
        SubmitNodeCollector submitNodeCollector,
        //?} else {
        /*MultiBufferSource bufferSource,
        int color,
        *///?}
        CallbackInfo ci
    ) {
        if (new BlockOverlayRenderEvent(OverlayType.BLOCK).post()) ci.cancel();
    }
}
