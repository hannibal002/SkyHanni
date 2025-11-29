package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public class MixinScreen {

    // NOTE: 1.21.9+ Mojmap name is renderWithTooltipAndSubtitles
    @WrapOperation(method = "renderWithTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void wrapRenderBackground(Screen instance, DrawContext context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        original.call(instance, context, mouseX, mouseY, deltaTicks);
        new ScreenDrawnEvent(context, MinecraftClient.getInstance().currentScreen).post();
    }
}
