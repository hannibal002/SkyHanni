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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen {

    @Shadow
    protected abstract List<Text> getTooltipFromItem(ItemStack par1);

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

    @Inject(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V", shift = At.Shift.AFTER))
    private void renderBackground(DrawContext drawContext, int x, int y, CallbackInfo ci, @Local ItemStack itemStack) {
        List<Text> textTooltip = getTooltipFromItem(itemStack);
        List<String> tooltip = new ArrayList<>();
        for (Text text : textTooltip) {
            tooltip.add(TextCompatKt.formattedTextCompat(text));
        }
        ToolTipData.getTooltip(itemStack, tooltip);
        ToolTipData.onHover(drawContext, itemStack, tooltip);
        GuiScreenHookKt.renderToolTip(drawContext, itemStack);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (new GuiKeyPressEvent((HandledScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
    }
}
