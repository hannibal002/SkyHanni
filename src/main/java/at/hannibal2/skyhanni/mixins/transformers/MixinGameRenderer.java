package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Unique
    private GuiGraphicsExtractor skyhanni$guiGraphics;

    @ModifyVariable(method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "STORE"), name = "graphics")
    private GuiGraphicsExtractor skyhanni$captureGuiGraphicsExtractor(GuiGraphicsExtractor graphics) {
        skyhanni$guiGraphics = graphics;
        return graphics;
    }

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
}
