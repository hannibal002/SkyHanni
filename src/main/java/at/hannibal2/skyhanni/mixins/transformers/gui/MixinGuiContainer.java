package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.mixins.hooks.GuiContainerHook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC > 1.21.8
//$$ import net.minecraft.client.input.KeyEvent;
//#endif

@Mixin(AbstractContainerScreen.class)
public abstract class MixinGuiContainer<T extends AbstractContainerMenu> extends Screen {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    protected MixinGuiContainer(Component title) {
        super(title);
    }

    @Unique
    private final GuiContainerHook skyHanni$hook = new GuiContainerHook(this);

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;onClose()V", shift = At.Shift.BEFORE), cancellable = true)
    //#if MC < 1.21.9
    private void closeWindowPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        //#else
        //$$ private void closeWindowPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        //#endif
        skyHanni$hook.closeWindowPressed(cir);
    }

    //#if MC < 1.21.6
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    //#else
    //$$ @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    //#endif
    private void backgroundDrawn(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.backgroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void preDraw(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.preDraw(context, mouseX, mouseY, deltaTicks, ci);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postDraw(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.postDraw(context, mouseX, mouseY, deltaTicks);
    }

    //#if MC < 1.21.6
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
    //#endif
    private void onForegroundDraw(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.foregroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(GuiGraphics context, Slot slot, CallbackInfo ci) {
        skyHanni$hook.onDrawSlot(slot, ci);
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void onDrawSlotReturn(GuiGraphics context, Slot slot, CallbackInfo ci) {
        skyHanni$hook.onDrawSlotPost(slot);
    }

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ClickType;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, ClickType actionType, CallbackInfo cir) {
        skyHanni$hook.onMouseClick(slot, slotId, button, actionType.id(), cir);
    }

    //#if MC < 1.21.6
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphics;)V", shift = At.Shift.AFTER))
    //#endif
    private void renderBackgroundTexture(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        ToolTipData.INSTANCE.setLastSlot(this.hoveredSlot);
    }

}
