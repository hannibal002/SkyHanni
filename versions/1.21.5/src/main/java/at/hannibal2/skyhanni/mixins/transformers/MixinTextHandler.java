package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import net.minecraft.client.font.TextHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextHandler.class)
public class MixinTextHandler {

    @ModifyArg(
        method = "method_27487",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/font/TextHandler$StyledString;<init>(Ljava/lang/String;Lnet/minecraft/text/Style;)V"
        )
    )
    private static String insideWrapLinesLambda(String literal) {

        return ModifyVisualWords.INSTANCE.modifyText(
            literal
        );
    }

}
