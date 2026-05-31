package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ChatHoverEvent;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import at.hannibal2.skyhanni.mixins.hooks.RenderItemHookKt;
import at.hannibal2.skyhanni.mixins.hooks.VisualWordsHook;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
// prevent replacement
//~ if < 26.1 'MixinGuiGraphicsExtractor' -> 'MixinGuiGraphicsExtractor'
public abstract class MixinGuiGraphicsExtractor {

    //~ if < 26.1 'item(' -> 'renderItem('
    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
    private void drawItemPost(LivingEntity entity, Level world, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        RenderItemHookKt.renderItemReturn((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }

    //~ if < 26.1 'itemDecorations' -> 'renderItemDecorations'
    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void drawItemDecorationsPost(Font textRenderer, ItemStack stack, int x, int y, String stackCountText, CallbackInfo ci) {
        RenderItemHookKt.renderItemOverlayPost((GuiGraphicsExtractor) (Object) this, stack, x, y, stackCountText);
    }

    //~ if < 26.1 'componentHoverEffect' -> 'renderComponentHoverEffect'
    @Inject(method = "componentHoverEffect", at = @At(value = "INVOKE", target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    private void onRenderComponentHoverEffect(Font font, Style style, int i, int j, CallbackInfo ci) {
        GuiChatHook.setReplacementComponent(null);
        new ChatHoverEvent(style.getHoverEvent()).post();
    }

    //~ if < 26.1 'componentHoverEffect' -> 'renderComponentHoverEffect'
    @ModifyArg(method = "componentHoverEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), index = 0)
    private FormattedText replaceWithNewList(FormattedText originalComponent) {
        return GuiChatHook.getReplacementComponent() != null ? GuiChatHook.getReplacement() : originalComponent;
    }

    @ModifyArg(
        //~ if < 26.1 'text' -> 'drawString'
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At(
            value = "INVOKE",
            //~ if < 26.1 'renderer/state/gui' -> 'gui/render/state'
            //~ if < 26.1 'addText' -> 'submitText'
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addText(Lnet/minecraft/client/renderer/state/gui/GuiTextRenderState;)V"
        ),
        index = 0
    )
    private GuiTextRenderState modifyVisualWordsTextState(GuiTextRenderState textState) {
        if (!VisualWordsHook.isCaxtonLoaded()) return textState;
        FormattedCharSequence text = VisualWordsHook.modifyOrderedText(textState.text);
        return text == textState.text ? textState : new GuiTextRenderState(
            textState.font,
            text,
            textState.pose,
            textState.x,
            textState.y,
            textState.color,
            textState.backgroundColor,
            textState.dropShadow,
            false,
            textState.scissor
        );
    }

}
