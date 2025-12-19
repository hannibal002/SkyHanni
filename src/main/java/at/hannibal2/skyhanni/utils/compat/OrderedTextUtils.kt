package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.StringDecomposer
import kotlin.time.Duration.Companion.minutes

object OrderedTextUtils {
    private val textToLegacyCache = TimeLimitedCache<FormattedCharSequence, String>(5.minutes)

    @JvmStatic
    fun orderedTextToLegacyString(orderedText: FormattedCharSequence?): String {
        orderedText ?: return ""

        return textToLegacyCache.getOrPut(orderedText) {
            val builder = StringBuilder()
            var lastStyle = Style.EMPTY
            orderedText.accept { _, style, codePoint ->
                if (codePoint == -1) return@accept true

                if (lastStyle != style) {
                    builder.append(requiredStyleChangeString(lastStyle, style, true))
                    lastStyle = style
                }

                builder.appendCodePoint(codePoint)
                true
            }

            return builder.toString().removeSuffix("§r").removePrefix("§r")
        }
    }

    private val legacyToTextCache = TimeLimitedCache<String, FormattedCharSequence>(5.minutes)

    @JvmStatic
    fun legacyTextToOrderedText(legacyString: String): FormattedCharSequence {

        return legacyToTextCache.getOrPut(legacyString) {
            val isNoReplace = legacyString.startsWith("§§")

            FormattedCharSequence { visitor ->
                if (isNoReplace) visitor.accept(0, Style.EMPTY, -1)

                StringDecomposer.iterateFormatted(legacyString, Style.EMPTY) { index: Int, style: Style, codePoint: Int ->
                    visitor.accept(index, style, codePoint)
                    true
                }

                true
            }
        }
    }

    fun requiredStyleChangeString(from: Style, to: Style, exclusive: Boolean = false): String {
        val reset = (
            from.isBold && !to.isBold ||
                from.isItalic && !to.isItalic ||
                from.isObfuscated && !to.isObfuscated ||
                from.isUnderlined && !to.isUnderlined ||
                from.isStrikethrough && !to.isStrikethrough ||
                from.color != null && to.color == null ||
                exclusive && (from.color != to.color)
            )

        val sb = StringBuilder()

        if (((from.color != to.color) && to.color != null) || (reset && to.color != null)) {
            if (!exclusive) sb.append(ChatFormatting.RESET.toString())

            if (to.color?.name == "chroma") {
                sb.append("§z")
            } else {

                val colorFormatting = to.color?.toChatFormatting()

                if (colorFormatting != null) {
                    sb.append(colorFormatting.toString())
                }
            }
        } else if (reset) {
            sb.append(ChatFormatting.RESET.toString())
        }

        if ((to.isBold && reset) || (to.isBold && !from.isBold)) {
            sb.append(ChatFormatting.BOLD.toString())
        }
        if ((to.isItalic && reset) || (to.isItalic && !from.isItalic)) {
            sb.append(ChatFormatting.ITALIC.toString())
        }
        if ((to.isObfuscated && reset) || (to.isObfuscated && !from.isObfuscated)) {
            sb.append(ChatFormatting.OBFUSCATED.toString())
        }
        if ((to.isUnderlined && reset) || (to.isUnderlined && !from.isUnderlined)) {
            sb.append(ChatFormatting.UNDERLINE.toString())
        }
        if ((to.isStrikethrough && reset) || (to.isStrikethrough && !from.isStrikethrough)) {
            sb.append(ChatFormatting.STRIKETHROUGH.toString())
        }

        return sb.toString()
    }
}
