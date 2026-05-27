package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.VisualWordsHook;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
//? if > 1.21.10
//import java.util.function.BiConsumer;

@Mixin(StringSplitter.class)
public class MixinTextHandler {

    //? if < 1.21.11 {
    @WrapMethod(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/FormattedText;)Ljava/util/List;"
    )
    private List<FormattedText> dontWrapOtherLines(FormattedText text, int maxWidth, Style style, FormattedText wrappedLinePrefix, Operation<List<FormattedText>> original) {
        return VisualWordsHook.INSTANCE.withoutWordChanges(() -> original.call(text, maxWidth, style, wrappedLinePrefix));
    }
    //? }

    @WrapMethod(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;)Ljava/util/List;"
    )
    private List<FormattedText> dontWrapOtherLines(FormattedText text, int maxWidth, Style style, Operation<List<FormattedText>> original) {
        return VisualWordsHook.INSTANCE.withoutWordChanges(() -> original.call(text, maxWidth, style));
    }

    @ModifyVariable(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Ljava/util/function/BiConsumer;)V",
        at = @At(value = "HEAD"),
        index = 1,
        argsOnly = true
    )
    private FormattedText modifyStringVisitable(FormattedText visitable) {
        return VisualWordsHook.INSTANCE.modifyFormattedText(visitable);
    }

}
