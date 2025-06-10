package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiData;
import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent;
import at.hannibal2.skyhanni.events.GuiContainerEvent;
import at.hannibal2.skyhanni.events.GuiKeyPressEvent;
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent;
import at.hannibal2.skyhanni.mixins.hooks.GuiScreenHookKt;
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import at.hannibal2.skyhanni.utils.compat.TextCompatKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// todo 1.21 impl needed
@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen {

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void renderHead(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!SkyHanniDebugsAndTests.INSTANCE.getGlobalRender()) return;
        HandledScreen<?> gui = (HandledScreen<?>) (Object) this;
        if (new GuiContainerEvent.PreDraw(context, gui, gui.getScreenHandler(), mouseX, mouseY, deltaTicks).post()) {
            GuiData.INSTANCE.setPreDrawEventCancelled(true);
            ci.cancel();
        } else {
            DelayedRun.INSTANCE.runNextTick(() -> {
                GuiData.INSTANCE.setPreDrawEventCancelled(false);
                return null;
            });
        }
    }

    @Inject(method = "render", at = @At(value = "TAIL"), cancellable = true)
    private void renderTail(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (new DrawScreenAfterEvent(context, mouseX, mouseY, ci).post()) ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    private void renderBackgroundTexture(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (MinecraftCompat.INSTANCE.getLocalWorldExists() && MinecraftCompat.INSTANCE.getLocalPlayerExists()) {
            new DrawBackgroundEvent(context).post();
        }
    }

    @ModifyArg(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V"), index = 1)
    private List<Text> renderBackground(List<Text> textTooltip, @Local ItemStack itemStack, @Local(argsOnly = true) DrawContext drawContext) {
        List<String> tooltip = new ArrayList<>();
        for (Text text : textTooltip) {
            tooltip.add(TextCompatKt.formattedTextCompat(text));
        }
        List<String> beforeTooltip = new ArrayList<>(tooltip);
        ToolTipData.getTooltip(itemStack, tooltip);
        ToolTipData.onHover(drawContext, itemStack, tooltip);
        GuiScreenHookKt.renderToolTip(drawContext, itemStack);
        if (tooltip.equals(beforeTooltip)) return textTooltip;
        List<Text> newTooltip = new ArrayList<>();
        if (tooltip.isEmpty()) return newTooltip;
        for (int i = 0; i < tooltip.size(); i++) {
            // todo this approach sucks but idk see a better way without recoding everywhere to use text
            if (beforeTooltip.size() > i && beforeTooltip.get(i).equals(tooltip.get(i))) {
                newTooltip.add(textTooltip.get(i));
            } else {
                newTooltip.add(Text.of(tooltip.get(i)));
            }
        }
        return newTooltip;
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (new GuiKeyPressEvent((HandledScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
    }
}
