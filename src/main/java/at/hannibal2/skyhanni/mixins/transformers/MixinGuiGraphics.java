package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe;
import at.hannibal2.skyhanni.mixins.hooks.RenderItemHookKt;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(GuiGraphics.class)
public class MixinGuiGraphics {

    //#if MC < 1.21.6
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("RETURN"))
    private void drawItemPost(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
    //#else
    //$$ @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
    //$$ private void drawItemPost(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
    //#endif
        RenderItemHookKt.renderItemReturn((GuiGraphics) (Object) this, stack, x, y);
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void drawItemPost(Font textRenderer, ItemStack stack, int x, int y, String stackCountText, CallbackInfo ci) {
        RenderItemHookKt.renderItemOverlayPost((GuiGraphics) (Object) this, stack, x, y, stackCountText);
    }


    @ModifyExpressionValue(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;getTooltipFromItem(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private List<Component> sh$renderTooltipInternal(List<Component> textTooltip, @Local(argsOnly = true) ItemStack itemStack, @Local(argsOnly = true) Font font) {
        if (CustomWardrobe.shouldHideNormalTooltip()) {
            return new ArrayList<>();
        }
        return ToolTipData.processModernTooltip((GuiGraphics) (Object) this, itemStack, textTooltip);
    }
}
