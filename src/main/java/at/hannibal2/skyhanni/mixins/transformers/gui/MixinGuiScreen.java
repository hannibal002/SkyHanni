package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.mixins.hooks.GuiScreenHookKt;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @Inject(method = "renderToolTip", at = @At("TAIL"))
    public void renderToolTip(ItemStack stack, int x, int y, CallbackInfo ci) {
        GuiScreenHookKt.renderToolTip(stack);
    }

    @Inject(method = "renderToolTip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getRarity()Lnet/minecraft/item/EnumRarity;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void getTooltip(ItemStack stack, int x, int y, CallbackInfo ci, List<String> list, int i) {
        ToolTipData.getTooltip(stack, list);
    }
}
