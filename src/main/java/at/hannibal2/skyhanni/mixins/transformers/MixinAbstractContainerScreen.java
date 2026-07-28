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
import at.hannibal2.skyhanni.mixins.hooks.GuiContainerHook;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.KeyboardManager;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Unique
    private final GuiContainerHook skyhanni$hook = new GuiContainerHook(this);

    protected MixinAbstractContainerScreen(Component title) {
        super(title);
    }

    //~ if < 26.1 '"extractRenderState"' -> '"render"' {
    @Inject(method = "extractRenderState", at = @At(value = "HEAD"), cancellable = true)
    private void renderHead(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (GlobalRender.getRenderDisabled()) return;
        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) (Object) this;
        if (new GuiContainerEvent.PreDraw(context, gui, gui.getMenu(), mouseX, mouseY, deltaTicks).post().isCancelled()) {
            GuiData.setPreDrawEventCancelled(true);
            ci.cancel();
        } else {
            DelayedRun.runNextTick(() -> GuiData.setPreDrawEventCancelled(false));
        }
    }

    @Inject(method = "extractRenderState", at = @At(value = "TAIL"), cancellable = true)
    private void renderTail(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (new DrawScreenAfterEvent(context, mouseX, mouseY, ci).post().isCancelled()) ci.cancel();
    }

    @Inject(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER))
    private void renderBackgroundTexture(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (MinecraftCompat.getLocalWorldExists() && MinecraftCompat.getLocalPlayerExists()) {
            new DrawBackgroundEvent(context).post();
        }
    }
    //~}

    //~ if < 26.1 'extractTooltip' -> 'renderTooltip'
    @ModifyArg(method = "extractTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"), index = 1)
    private List<Component> renderBackground(List<Component> textTooltip, @Local ItemStack itemStack, @Local(argsOnly = true) GuiGraphicsExtractor drawContext) {
        return ToolTipData.processModernTooltip(drawContext, itemStack, textTooltip);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.input();
        TextInput.Companion.onGuiInput(cir);
        boolean shouldCancelInventoryClose = KeyboardManager.checkIsInventoryClosure(keyCode);
        if (new GuiKeyPressEvent((AbstractContainerScreen<?>) (Object) this).post().isCancelled() || shouldCancelInventoryClose) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (new GuiKeyPressEvent(screen).post().isCancelled()) {
            cir.setReturnValue(false);
        }
        if (new GuiMouseInputEvent(screen).post().isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    //~ if < 26.1 'extractLabels' -> 'renderLabels'
    @ModifyArg(method = "extractLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4)
    private int customForegroundTextColor(int colour) {
        return BetterContainers.getTextColor(colour);
    }

    @WrapOperation(
        method = "extractSlotHighlightBack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z")
    )
    private boolean canBeHighlightedBack(Slot slot, Operation<Boolean> original) {
        return BetterContainers.slotCanBeHighlighted(slot, original.call(slot));
    }

    @WrapOperation(
        method = "extractSlotHighlightFront",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;isHighlightable()Z")
    )
    private boolean canBeHighlightedFront(Slot slot, Operation<Boolean> original) {
        return BetterContainers.slotCanBeHighlighted(slot, original.call(slot));
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private boolean fixMiddleClick(boolean original) {
        if (!MiddleClickFix.INSTANCE.isEnabled()) return original;
        return true;
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;onClose()V", shift = At.Shift.BEFORE), cancellable = true)
    private void closeWindowPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        skyhanni$hook.closeWindowPressed(cir);
    }

    @Inject(
        method = "extractContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
        )
    )
    private void backgroundDrawn(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyhanni$hook.backgroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

    //~ if < 26.1 'extractRenderState' -> 'render' {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void preDraw(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyhanni$hook.preDraw(context, mouseX, mouseY, deltaTicks, ci);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void postDraw(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyhanni$hook.postDraw(context, mouseX, mouseY, deltaTicks);
    }
    //~}

    @Inject(
        method = "extractContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onForegroundDraw(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyhanni$hook.foregroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(GuiGraphicsExtractor guiGraphics, Slot slot, int i, int j, CallbackInfo ci) {
        skyhanni$hook.onDrawSlot(slot, ci);
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void onDrawSlotReturn(GuiGraphicsExtractor guiGraphics, Slot slot, int i, int j, CallbackInfo ci) {
        skyhanni$hook.onDrawSlotPost(slot);
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo cir) {
        skyhanni$hook.onMouseClick(slot, slotId, button, actionType, cir);
    }

    @Inject(
        method = "extractContents",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            shift = At.Shift.AFTER
        )
    )
    private void renderTooltipBackgroundTexture(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        ToolTipData.INSTANCE.setLastSlot(this.hoveredSlot);
    }
}
