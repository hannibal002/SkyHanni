package at.lorenz.mod.mixins;

import at.lorenz.mod.mixinhooks.RenderItemHookKt;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    //    @Inject(method = "renderItemIntoGUI", at = @At("HEAD"))
    //    private void renderRarity(ItemStack stack, int x, int y, CallbackInfo ci) {
    //        RenderItemHookKt.renderRarity(stack, x, y, ci);
    //    }

    @Inject(method = "renderItemOverlayIntoGUI", at = @At("RETURN"))
    private void renderItemOverlayPost(FontRenderer fr, ItemStack stack, int xPosition, int yPosition, String text, CallbackInfo ci) {
        RenderItemHookKt.renderItemOverlayPost(fr, stack, xPosition, yPosition, text, ci);
    }

    //    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/GlStateManager.scale(FFF)V", shift = At.Shift.AFTER))
    //    private void renderItemPre(ItemStack stack, IBakedModel model, CallbackInfo ci) {
    //        RenderItemHookKt.renderItemPre(stack, model, ci);
    //    }
    //
    //    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;renderEffect(Lnet/minecraft/client/resources/model/IBakedModel;)V", shift = At.Shift.BEFORE), cancellable = true)
    //    private void modifyGlintRendering(ItemStack stack, IBakedModel model, CallbackInfo ci) {
    //        RenderItemHookKt.modifyGlintRendering(stack, model, ci);
    //    }

    @Inject(method = "renderEffect", at = @At("HEAD"), cancellable = true)
    public void onRenderEffect(CallbackInfo ci) {
        if (RenderItemHookKt.getSkipGlint()) {
            ci.cancel();
        }
    }
}
