package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.mixins.hooks.GuiContainerHook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.input.KeyEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinGuiContainer<T extends AbstractContainerMenu> extends Screen {

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    protected MixinGuiContainer(Component title) {
        super(title);
    }

    @Unique
    private final GuiContainerHook skyhanni$hook = new GuiContainerHook(this);

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
    private void renderBackgroundTexture(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        ToolTipData.INSTANCE.setLastSlot(this.hoveredSlot);
    }
}
