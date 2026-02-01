package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GlobalRender;
import at.hannibal2.skyhanni.data.GuiData;
import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent;
import at.hannibal2.skyhanni.events.GuiContainerEvent;
import at.hannibal2.skyhanni.events.GuiKeyPressEvent;
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent;
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent;
import at.hannibal2.skyhanni.features.inventory.BetterContainers;
import at.hannibal2.skyhanni.features.inventory.MiddleClickFix;
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.KeyboardManager;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen {

    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void renderHead(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (GlobalRender.INSTANCE.getRenderDisabled()) return;
        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) (Object) this;
        if (new GuiContainerEvent.PreDraw(context, gui, gui.getMenu(), mouseX, mouseY, deltaTicks).post()) {
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
    private void renderTail(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (new DrawScreenAfterEvent(context, mouseX, mouseY, ci).post()) ci.cancel();
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void renderBackgroundTexture(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (MinecraftCompat.INSTANCE.getLocalWorldExists() && MinecraftCompat.INSTANCE.getLocalPlayerExists()) {
            new DrawBackgroundEvent(context).post();
        }
    }

    @ModifyArg(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"), index = 1)
    private List<Component> renderBackground(List<Component> textTooltip, @Local ItemStack itemStack, @Local(argsOnly = true) GuiGraphics drawContext) {
        if (CustomWardrobe.shouldHideNormalTooltip()) {
            return new ArrayList<>();
        }
        return ToolTipData.processModernTooltip(drawContext, itemStack, textTooltip);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.input();
        TextInput.Companion.onGuiInput(cir);
        boolean shouldCancelInventoryClose = KeyboardManager.checkIsInventoryClosure(keyCode);
        if (new GuiKeyPressEvent((AbstractContainerScreen<?>) (Object) this).post() || shouldCancelInventoryClose) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (new GuiKeyPressEvent((AbstractContainerScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
        if (new GuiMouseInputEvent((AbstractContainerScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4)
    private int customForegroundTextColor(int colour) {
        return BetterContainers.getTextColor(colour);
    }

    @Redirect(method = "renderSlotHighlightBack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z"))
    private boolean canBeHighlightedBack(Slot slot) {
        return BetterContainers.slotCanBeHighlighted(slot);
    }

    @Redirect(method = "renderSlotHighlightFront", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z"))
    private boolean canBeHighlightedFront(Slot slot) {
        return BetterContainers.slotCanBeHighlighted(slot);
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private boolean fixMiddleClick(boolean original) {
        if (!MiddleClickFix.INSTANCE.isEnabled()) return original;
        return true;
    }
}
