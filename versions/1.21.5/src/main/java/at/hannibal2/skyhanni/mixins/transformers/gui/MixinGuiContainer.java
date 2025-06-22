package at.hannibal2.skyhanni.mixins.transformers.gui;


import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.mixins.hooks.GuiContainerHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class MixinGuiContainer<T extends ScreenHandler> extends Screen {

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    protected MixinGuiContainer(Text title) {
        super(title);
    }

    @Unique
    private final GuiContainerHook skyHanni$hook = new GuiContainerHook(this);

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;close()V", shift = At.Shift.BEFORE), cancellable = true)
    private void closeWindowPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        skyHanni$hook.closeWindowPressed(cir);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlightBack(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void backgroundDrawn(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.backgroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void preDraw(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.preDraw(context, mouseX, mouseY, deltaTicks, ci);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postDraw(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.postDraw(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.AFTER))
    private void onForegroundDraw(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.foregroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        skyHanni$hook.onDrawSlot(slot, ci);
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void onDrawSlotReturn(DrawContext context, Slot slot, CallbackInfo ci) {
        skyHanni$hook.onDrawSlotPost(slot);
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo cir) {
        skyHanni$hook.onMouseClick(slot, slotId, button, actionType.getIndex(), cir);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlightBack(Lnet/minecraft/client/gui/DrawContext;)V", shift = At.Shift.AFTER))
    private void renderBackgroundTexture(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        ToolTipData.INSTANCE.setLastSlot(this.focusedSlot);
    }

}
