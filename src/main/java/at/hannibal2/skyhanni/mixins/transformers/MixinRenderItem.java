package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderItemHookKt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    @Inject(method = "renderItemOverlayIntoGUI", at = @At("RETURN"))
    private void renderItemOverlayPost(TextRenderer fr, ItemStack stack, int xPosition, int yPosition, String text, CallbackInfo ci) {
        RenderItemHookKt.renderItemOverlayPost(new DrawContext(), stack, xPosition, yPosition, text);
    }

    @Inject(method = "renderItemIntoGUI", at = @At("RETURN"))
    public void renderItemReturn(ItemStack stack, int x, int y, CallbackInfo ci) {
        RenderItemHookKt.renderItemReturn(new DrawContext(), stack, x, y);
    }
}
