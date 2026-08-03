package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @WrapOperation(
        method = "extractRenderStateWithTooltipAndSubtitles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"
        )
    )
    private void wrapExtractBackground(Screen instance, GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, Operation<Void> original) {
        original.call(instance, context, mouseX, mouseY, deltaTicks);
        new ScreenDrawnEvent(context, Minecraft.getInstance().screen).post();
    }
}
