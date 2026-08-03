package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import at.hannibal2.skyhanni.utils.system.PlatformUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.gui.Gui;
//?} else {
/*import net.minecraft.client.renderer.GameRenderer;
*///?}

//? if < 26.1 {
/*import com.llamalad7.mixinextras.sugar.Local;
*///?}

//~ if < 26.2 'Gui' -> 'GameRenderer'
@Mixin(Gui.class)
public abstract class MixinGui_GameRenderer {

    //? if >= 26.1 {
    @Unique
    private GuiGraphicsExtractor skyhanni$guiGraphics;

    //~ if < 26.2 'extractRenderState' -> 'extractGui'
    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At("STORE"), name = "graphics")
    private GuiGraphicsExtractor skyhanni$captureGuiGraphicsExtractor(GuiGraphicsExtractor graphics) {
        skyhanni$guiGraphics = graphics;
        return graphics;
    }
    //?}

    //? if >= 26.1 {
    //~ if < 26.2 '"extractRenderState' -> '"extractGui' {
    //~ if < 26.2 'Hud;' -> 'Gui;' {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onRenderStartPhase(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$guiGraphics, true).post();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onRenderEndPhase(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$guiGraphics, false).post();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void onRenderTail(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        GuiEditManager.renderLast(skyhanni$guiGraphics);
    }
    //~}
    //~}
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
