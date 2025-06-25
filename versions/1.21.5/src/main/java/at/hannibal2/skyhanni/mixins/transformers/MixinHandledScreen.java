package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiData;
import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent;
import at.hannibal2.skyhanni.events.GuiContainerEvent;
import at.hannibal2.skyhanni.events.GuiKeyPressEvent;
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent;
import at.hannibal2.skyhanni.features.inventory.HideNotClickableItems;
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeApi;
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests;
import at.hannibal2.skyhanni.utils.DelayedRun;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> {

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
        if (WardrobeApi.INSTANCE.getInCustomWardrobe()) {
            return new ArrayList<>();
        }
        return ToolTipData.processModernTooltip(drawContext, itemStack, textTooltip);
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        TextInput.Companion.onGuiInput(cir);
        if (new GuiKeyPressEvent((HandledScreen<?>) (Object) this).post()) {
            cir.setReturnValue(false);
        }
    }

    // these fields exist in HandledScreen
    protected int x;
    protected int y;
    protected T handler;

    @Inject(
        method = "drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
        at = @At("TAIL")
    )
    private void onDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
//         GuiContainerEvent.ForegroundDrawnEvent(context, handler, handler., mouseX, mouseY, partialTicks).post()

//         handler.slots

        HideNotClickableItems.test(handler.slots, context, this.x, this.y);


        // semi-transparent gray: 0x88000000
//         for (Slot slot : handler.slots) {
//             int sx = this.x + slot.x;
//             int sy = this.y + slot.y;
//             context.fill(sx, sy, sx + 16, sy + 16, 0x88000000);
//         }
    }
}
