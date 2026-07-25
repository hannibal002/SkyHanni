package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.1 {
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.Unique;
//?}

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    //? if >= 26.1 {
    @Unique
    private GuiGraphicsExtractor skyhanni$guiGraphics;

    @ModifyVariable(method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "STORE"), name = "graphics")
    private GuiGraphicsExtractor skyhanni$captureGuiGraphicsExtractor(GuiGraphicsExtractor graphics) {
        skyhanni$guiGraphics = graphics;
        return graphics;
    }
    //?}

    //? if >= 26.1 {
    @Inject(method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onRenderStartPhase(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$guiGraphics, true).post();
    }

    @Inject(method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onRenderEndPhase(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$guiGraphics, false).post();
    }

    @Inject(method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void onRenderTail(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        GuiEditManager.renderLast(skyhanni$guiGraphics);
    }
    //?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"))
    private void onRenderStartPhase(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(context, true).post();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onRenderEndPhase(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(context, false).post();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void onRenderTail(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        GuiEditManager.renderLast(context);
    }
    *///?}
}
