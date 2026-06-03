//? if < 26.1 {
/*package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererLegacy121 {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"))
    private void skyhanni$onRenderStartPhase(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(context, true).post();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void skyhanni$onRenderEndPhase(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(context, false).post();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void skyhanni$onRenderTail(DeltaTracker tickCounter, boolean tick, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        GuiEditManager.renderLast(context);
    }
}
*///?}
