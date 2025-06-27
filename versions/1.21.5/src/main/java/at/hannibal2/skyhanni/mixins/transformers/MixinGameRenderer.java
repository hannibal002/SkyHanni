package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V"))
    private void onRenderStartPhase(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
        if (MinecraftCompat.INSTANCE.getLocalPlayerExists()) new RenderingTickEvent(context, true).post();
    }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderAutosaveIndicator(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void onRenderEndPhase(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
       if (MinecraftCompat.INSTANCE.getLocalPlayerExists()) new RenderingTickEvent(context, false).post();
   }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;draw()V"))
    private void onRenderTail(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
        GuiEditManager.renderLast(context);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void onRenderTooltip(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
         new ScreenDrawnEvent(context, MinecraftClient.getInstance().currentScreen).post();
    }
}
