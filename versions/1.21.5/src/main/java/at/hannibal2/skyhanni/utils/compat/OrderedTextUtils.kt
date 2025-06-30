package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.TextVisitFactory
import net.minecraft.util.Formatting
import kotlin.time.Duration.Companion.minutes

object OrderedTextUtils {
    private val textToLegacyCache = TimeLimitedCache<OrderedText, String>(5.minutes)

    @JvmStatic
    fun orderedTextToLegacyString(orderedText: OrderedText?): String {
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

    private val legacyToTextCache = TimeLimitedCache<String, OrderedText>(5.minutes)

    @JvmStatic
    fun legacyTextToOrderedText(legacyString: String): OrderedText {

        return legacyToTextCache.getOrPut(legacyString) {
            val isNoReplace = legacyString.startsWith("§§")

            OrderedText { visitor ->
                if (isNoReplace) visitor.accept(0, Style.EMPTY, -1)

                TextVisitFactory.visitFormatted(legacyString, Style.EMPTY) { index: Int, style: Style, codePoint: Int ->
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
            if (!exclusive) sb.append(Formatting.RESET.toString())

            if (to.color?.name == "chroma") {
                sb.append("§z")
            } else {

                val colorFormatting = to.color?.toChatFormatting()

                if (colorFormatting != null) {
                    sb.append(colorFormatting.toString())
                }
            }
        } else if (reset) {
            sb.append(Formatting.RESET.toString())
        }

        if ((to.isBold && reset) || (to.isBold && !from.isBold)) {
            sb.append(Formatting.BOLD.toString())
        }
        if ((to.isItalic && reset) || (to.isItalic && !from.isItalic)) {
            sb.append(Formatting.ITALIC.toString())
        }
        if ((to.isObfuscated && reset) || (to.isObfuscated && !from.isObfuscated)) {
            sb.append(Formatting.OBFUSCATED.toString())
        }
        if ((to.isUnderlined && reset) || (to.isUnderlined && !from.isUnderlined)) {
            sb.append(Formatting.UNDERLINE.toString())
        }
        if ((to.isStrikethrough && reset) || (to.isStrikethrough && !from.isStrikethrough)) {
            sb.append(Formatting.STRIKETHROUGH.toString())
        }

        return sb.toString()
    }
}
