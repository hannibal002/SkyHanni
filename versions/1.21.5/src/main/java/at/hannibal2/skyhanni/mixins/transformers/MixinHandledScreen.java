package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiData;
import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent;
import at.hannibal2.skyhanni.events.GuiContainerEvent;
import at.hannibal2.skyhanni.events.GuiKeyPressEvent;
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent;
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent;
import at.hannibal2.skyhanni.features.inventory.BetterContainers;
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe;
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.KeyboardManager;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

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

    //#if MC < 1.21.6
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "renderMain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    //#endif
    private void renderBackgroundTexture(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (MinecraftCompat.INSTANCE.getLocalWorldExists() && MinecraftCompat.INSTANCE.getLocalPlayerExists()) {
            new DrawBackgroundEvent(context).post();
        }
    }

    @ModifyArg(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/util/Identifier;)V"), index = 1)
    private List<Text> renderBackground(List<Text> textTooltip, @Local ItemStack itemStack, @Local(argsOnly = true) DrawContext drawContext) {
        if (CustomWardrobe.shouldHideNormalTooltip()) {
            return new ArrayList<>();
        }
        return ToolTipData.processModernTooltip(drawContext, itemStack, textTooltip);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        TextInput.Companion.onGuiInput(cir);
        boolean shouldCancelInventoryClose = KeyboardManager.checkIsInventoryClosure(keyCode);
        if (new GuiKeyPressEvent((HandledScreen<?>) (Object) this).post() || shouldCancelInventoryClose) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (new GuiKeyPressEvent((HandledScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
        if (new GuiMouseInputEvent((HandledScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
    }

    //#if MC < 1.21.6
    @ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"), index = 4)
    //#else
    //$$ @ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"), index = 4)
    //#endif
    private int customForegroundTextColor(int colour) {
        return BetterContainers.getTextColor(colour);
    }

    @Redirect(method = "drawSlotHighlightBack", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canBeHighlighted()Z"))
    private boolean canBeHighlightedBack(Slot slot) {
        return BetterContainers.slotCanBeHighlighted(slot);
    }

    @Redirect(method = "drawSlotHighlightFront", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canBeHighlighted()Z"))
    private boolean canBeHighlightedFront(Slot slot) {
        return BetterContainers.slotCanBeHighlighted(slot);
    }

}
