package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.mixins.hooks.ExtendedColorHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public class MixinTextVisitFactory {

    @WrapOperation(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;"
        )
    )
    private static ChatFormatting skipExtendedColorFormatting(
        char colorCode,
        Operation<ChatFormatting> original,
        @Local(argsOnly = true) String text,
        @Local(index = 7) int sectionIndex
    ) {
        if (ExtendedColorHook.shouldSkipLegacyFormatting(text, sectionIndex, colorCode)) {
            return null;
        }

        return original.call(colorCode);
    }

    @ModifyVariable(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;"
        ),
        ordinal = 2
    )
    private static Style onColorCodeCheck(
        Style style,
        @Local(argsOnly = true) String text,
        @Local(index = 7) int sectionIndex,
        @Local(index = 9) char colorCode
    ) {
        return ChromaFontManagerKt.setChromaColorStyle(
            ExtendedColorHook.applyExtendedColorStyle(style, text, sectionIndex, colorCode),
            text,
            colorCode
        );
    }

}
