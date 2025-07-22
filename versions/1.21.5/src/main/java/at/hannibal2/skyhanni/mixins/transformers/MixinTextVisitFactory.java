package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public class MixinTextVisitFactory {

    @ModifyVariable(
        method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/util/Formatting;byCode(C)Lnet/minecraft/util/Formatting;"
        ),
        ordinal = 2
    )
    private static Style onColorCodeCheck(Style style, @Local(argsOnly = true) String text, @Local(ordinal = 0) char colorCode) {
        return ChromaFontManagerKt.setChromaColorStyle(style, text, colorCode);
    }

}
