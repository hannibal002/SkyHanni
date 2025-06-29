package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(TextHandler.class)
public class MixinTextHandler {

    @WrapMethod(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;Lnet/minecraft/text/StringVisitable;)Ljava/util/List;"
    )
    private List<StringVisitable> dontWrapOtherLines(StringVisitable text, int maxWidth, Style style, StringVisitable wrappedLinePrefix, Operation<List<StringVisitable>> original) {
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        List<StringVisitable> lines = original.call(text, maxWidth, style, wrappedLinePrefix);

        ModifyVisualWords.INSTANCE.setChangeWords(true);
        return lines;
    }

    @WrapMethod(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;)Ljava/util/List;"
    )
    private List<StringVisitable> dontWrapOtherLines(StringVisitable text, int maxWidth, Style style, Operation<List<StringVisitable>> original) {
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        List<StringVisitable> lines = original.call(text, maxWidth, style);

        ModifyVisualWords.INSTANCE.setChangeWords(true);
        return lines;
    }

    @ModifyVariable(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;Ljava/util/function/BiConsumer;)V",
        at = @At(
            value = "HEAD"
        ),
        index = 1,
        argsOnly = true
    )
    private StringVisitable modifyStringVisitable(StringVisitable visitable) {

        return ModifyVisualWords.INSTANCE.transformStringVisitable(
            visitable
        );
    }

}
