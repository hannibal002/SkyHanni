package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"))
    private void onRenderStartPhase(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphics context) {
        if (MinecraftCompat.INSTANCE.getLocalPlayerExists()) new RenderingTickEvent(context, true).post();
    }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSavingIndicator(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void onRenderEndPhase(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphics context) {
       if (MinecraftCompat.INSTANCE.getLocalPlayerExists()) new RenderingTickEvent(context, false).post();
   }

    //? < 1.21.6 {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"))
    //?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    *///?}
    private void onRenderTail(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphics context) {
        GuiEditManager.renderLast(context);
    }

    //? < 1.21.6 {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void onRenderTooltip(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphics context) {
         new ScreenDrawnEvent(context, Minecraft.getInstance().screen).post();
    }
    //?}
}
