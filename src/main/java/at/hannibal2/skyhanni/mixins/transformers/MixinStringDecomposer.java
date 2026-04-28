package at.hannibal2.skyhanni.mixins.transformers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Implements §#RRGGBB§/ and §#RRGGBBAA§/ hex color codes by hooking into
 * {@link StringDecomposer#iterateFormatted}. Each hex digit is encoded as a
 * §-prefixed character, e.g. §#§6§a§e§e§4§8§/ renders text in RGB(0x6a, 0xee, 0x48).
 */
@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {

    @Unique private static final String HEX_CHARS = "0123456789abcdef";

    @Unique private static int skyhanni$hexState = -1;
    @Unique private static int skyhanni$hexValue = 0;
    @Unique private static int skyhanni$activeHexColor = -1;

    @Inject(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At("HEAD")
    )
    private static void skyhanni$resetColorState(CallbackInfoReturnable<Boolean> cir) {
        skyhanni$hexState = -1;
        skyhanni$hexValue = 0;
        skyhanni$activeHexColor = -1;
    }

    /**
     * Handles the §# (start) and §/ (end) markers of the hex color sequence.
     * These both produce null from {@link ChatFormatting#getByCode}, so they
     * are invisible to the normal format-code pipeline and must be caught here.
     *
     * @param d the character that followed the § sign
     * @param original the original getByCode call
     */
    @SuppressWarnings("InvokeAssignCanReplacedWithExpression")
    @WrapOperation(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/ChatFormatting;getByCode(C)Lnet/minecraft/ChatFormatting;"
        )
    )
    private static ChatFormatting skyhanni$onGetByCode(char d, Operation<ChatFormatting> original) {
        if (d == '#') {
            if (skyhanni$hexState == -1) {
                skyhanni$hexState = 0;
                skyhanni$hexValue = 0;
            }
        } else if (d == '/' && (skyhanni$hexState == 6 || skyhanni$hexState == 8)) {
            skyhanni$activeHexColor = skyhanni$hexValue & 0xFFFFFF;
            skyhanni$hexState = -1;
            skyhanni$hexValue = 0;
        }
        return original.call(d);
    }

    /**
     * When inside a hex sequence, prevents hex-digit §-codes from being applied
     * as standard Minecraft color formatting and instead accumulates their values.
     * Also resets the active hex color when an explicit color or §r is applied.
     *
     * @param style the style before applyLegacyFormat would modify it
     * @param chatFormatting the ChatFormatting value about to be applied
     * @param original the original applyLegacyFormat call
     */
    @WrapOperation(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Style;applyLegacyFormat(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;"
        )
    )
    private static Style skyhanni$onApplyLegacyFormat(Style style, ChatFormatting chatFormatting, Operation<Style> original) {
        if (skyhanni$hexState >= 0 && skyhanni$hexState < 8) {
            int hexDigit = HEX_CHARS.indexOf(chatFormatting.getChar());
            if (hexDigit >= 0) {
                skyhanni$hexState++;
                skyhanni$hexValue = (skyhanni$hexValue << 4) | hexDigit;
                return style;
            }
            skyhanni$hexState = -1;
            skyhanni$hexValue = 0;
        } else if (skyhanni$hexState >= 8) {
            skyhanni$hexState = -1;
            skyhanni$hexValue = 0;
        }
        if (chatFormatting.isColor() || chatFormatting == ChatFormatting.RESET) {
            skyhanni$activeHexColor = -1;
        }
        return original.call(style, chatFormatting);
    }

    @ModifyArg(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringDecomposer;feedChar(Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;IC)Z"
        ),
        index = 0
    )
    private static Style skyhanni$applyActiveHexColor(Style style) {
        if (skyhanni$activeHexColor == -1) return style;
        return style.withColor(TextColor.fromRgb(skyhanni$activeHexColor));
    }
}
