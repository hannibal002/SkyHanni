package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextHandler.class)
public class MixinTextHandler {

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
