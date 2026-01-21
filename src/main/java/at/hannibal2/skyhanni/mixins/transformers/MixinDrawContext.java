package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ChatHoverEvent;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import at.hannibal2.skyhanni.mixins.hooks.RenderItemHookKt;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class MixinDrawContext {

    //? < 1.21.6 {
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("RETURN"))
    private void drawItemPost(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
    //?} else {
    /*@Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
    private void drawItemPost(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
    *///?}
        RenderItemHookKt.renderItemReturn((GuiGraphics) (Object) this, stack, x, y);
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void drawItemPost(Font textRenderer, ItemStack stack, int x, int y, String stackCountText, CallbackInfo ci) {
        RenderItemHookKt.renderItemOverlayPost((GuiGraphics) (Object) this, stack, x, y, stackCountText);
    }

    @Inject(method = "renderComponentHoverEffect", at = @At(value = "INVOKE", target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void onRenderComponentHoverEffect(Font font, Style style, int i, int j, CallbackInfo ci) {
        GuiChatHook.INSTANCE.setReplacementComponent(null);
        new ChatHoverEvent(style.getHoverEvent()).post();
    }

    @ModifyArg(method = "renderComponentHoverEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), index = 0)
    private FormattedText replaceWithNewList(FormattedText originalComponent) {
        return GuiChatHook.INSTANCE.getReplacementComponent() != null ? GuiChatHook.INSTANCE.getReplacement() : originalComponent;
    }

}
