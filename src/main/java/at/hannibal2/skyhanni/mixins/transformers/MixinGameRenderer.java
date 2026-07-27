package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import at.hannibal2.skyhanni.mixins.hooks.GuiRendererHook;
import at.hannibal2.skyhanni.utils.render.RoundedShapeDrawer;
//?} else {
/*import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    //? if >= 26.2 {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;endFrame()V", shift = At.Shift.AFTER))
    private void skyhanni$clearChromaUniforms(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        GuiRendererHook.INSTANCE.clearChromaUniforms();
        RoundedShapeDrawer.INSTANCE.clearUniforms();
    }
    //?} else {
    /*@Inject(
        //? if >= 26.1 {
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
        //?} else {
        /^method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"
        )
        ^///?}
    )
    private void skyhanni$onRenderStartPhase(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        //? if >= 26.1 {
        boolean resourcesLoaded,
        //?}
        CallbackInfo ci,
        @Local GuiGraphicsExtractor guiGraphics
    ) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(guiGraphics, true).post();
    }

    @Inject(
        //? if >= 26.1 {
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        //?} else {
        /^method = "render",
        ^///?}
        at = @At(
            value = "INVOKE",
            //~ if < 26.1 'extract' -> 'render'
            target = "Lnet/minecraft/client/gui/Gui;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void skyhanni$onRenderEndPhase(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        //? if >= 26.1 {
        boolean resourcesLoaded,
        //?}
        CallbackInfo ci,
        @Local GuiGraphicsExtractor guiGraphics
    ) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(guiGraphics, false).post();
    }

    @Inject(
        //? if >= 26.1 {
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        //?} else {
        /^method = "render",
        ^///?}
        at = @At(
            value = "INVOKE",
            //~ if < 26.1 'extractRenderState' -> 'render'
            target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER
        )
    )
    private void skyhanni$onRenderTail(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        //? if >= 26.1 {
        boolean resourcesLoaded,
        //?}
        CallbackInfo ci,
        @Local GuiGraphicsExtractor guiGraphics
    ) {
        GuiEditManager.renderLast(guiGraphics);
    }
    *///?}
}
