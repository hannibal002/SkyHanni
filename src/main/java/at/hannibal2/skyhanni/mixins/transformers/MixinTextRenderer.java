package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.FormattedText;
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

        FormattedCharSequence replaced = ModifyVisualWords.INSTANCE.transformText(value);

        if (replaced == null) return value;
        return replaced;
    }

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", index = 1, at = @At("HEAD"), argsOnly = true)
    private String modifyString(String value) {

        FormattedCharSequence replaced = ModifyVisualWords.INSTANCE.transformText(OrderedTextUtils.legacyTextToOrderedText(value));

        if (replaced == null) return value;
        return OrderedTextUtils.orderedTextToLegacyString(replaced);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", index = 1, at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence modifyWidth(FormattedCharSequence value) {

        FormattedCharSequence replaced = ModifyVisualWords.INSTANCE.transformText(value);

        if (replaced == null) return value;
        return replaced;
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", index = 1, at = @At("HEAD"), argsOnly = true)
    private String modifyWidth(String value) {

        FormattedCharSequence replaced = ModifyVisualWords.INSTANCE.transformText(OrderedTextUtils.legacyTextToOrderedText(value));

        if (replaced == null) return value;
        return OrderedTextUtils.orderedTextToLegacyString(replaced);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", index = 1, at = @At("HEAD"), argsOnly = true)
    private FormattedText modifyWidth(FormattedText value) {

        FormattedText replaced = ModifyVisualWords.INSTANCE.transformStringVisitable(value);

        if (replaced == null) return value;
        return replaced;
    }
}
