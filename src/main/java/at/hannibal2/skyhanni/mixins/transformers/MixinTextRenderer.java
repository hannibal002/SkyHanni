package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.VisualWordsHook;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class MixinTextRenderer {

    //? if < 1.21.11 {
    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", index = 1, at = @At("HEAD"), argsOnly = true)
    //?} else
    //@ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", index = 1, at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence modifyOrderedText(FormattedCharSequence value) {
        return VisualWordsHook.INSTANCE.modifyOrderedText(value);
    }

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", index = 1, at = @At("HEAD"), argsOnly = true)
    private String modifyString(String value) {
        return VisualWordsHook.INSTANCE.modifyString(value);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", index = 1, at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence modifyWidthOrderedText(FormattedCharSequence value) {
        return VisualWordsHook.INSTANCE.modifyOrderedText(value);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", index = 1, at = @At("HEAD"), argsOnly = true)
    private String modifyWidthString(String value) {
        return VisualWordsHook.INSTANCE.modifyString(value);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", index = 1, at = @At("HEAD"), argsOnly = true)
    private FormattedText modifyWidthFormattedText(FormattedText value) {
        return VisualWordsHook.INSTANCE.modifyFormattedText(value);
    }
}
