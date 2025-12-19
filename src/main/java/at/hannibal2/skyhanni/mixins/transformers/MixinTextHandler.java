package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(StringSplitter.class)
public class MixinTextHandler {

    @WrapMethod(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/FormattedText;)Ljava/util/List;"
    )
    private List<FormattedText> dontWrapOtherLines(FormattedText text, int maxWidth, Style style, FormattedText wrappedLinePrefix, Operation<List<FormattedText>> original) {
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        List<FormattedText> lines = original.call(text, maxWidth, style, wrappedLinePrefix);

        ModifyVisualWords.INSTANCE.setChangeWords(true);
        return lines;
    }

    @WrapMethod(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;)Ljava/util/List;"
    )
    private List<FormattedText> dontWrapOtherLines(FormattedText text, int maxWidth, Style style, Operation<List<FormattedText>> original) {
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        List<FormattedText> lines = original.call(text, maxWidth, style);

        ModifyVisualWords.INSTANCE.setChangeWords(true);
        return lines;
    }

    @ModifyVariable(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Ljava/util/function/BiConsumer;)V",
        at = @At(
            value = "HEAD"
        ),
        index = 1,
        argsOnly = true
    )
    private FormattedText modifyStringVisitable(FormattedText visitable) {

        FormattedText replaced = ModifyVisualWords.INSTANCE.transformStringVisitable(
            visitable
        );

        if (replaced == null) return visitable;
        return replaced;
    }

}
