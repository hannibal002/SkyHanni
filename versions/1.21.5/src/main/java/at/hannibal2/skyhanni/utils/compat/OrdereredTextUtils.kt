package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.text.OrderedText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Style
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import java.util.Optional
import kotlin.time.Duration.Companion.minutes

object OrderedTextUtils {
    private val textToLegacyCache = TimeLimitedCache<OrderedText, String>(5.minutes)
    private val CHROMA_COLOR = TextColor(0xFFFFFF, "chroma")

    @JvmStatic
    fun orderedTextToLegacyString(orderedText: OrderedText?): String {
        orderedText ?: return ""

        return textToLegacyCache.getOrPut(orderedText) {
            val builder = StringBuilder()
            var lastStyle = Style.EMPTY
            orderedText.accept { _, style, codePoint ->
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

    @JvmStatic
    fun stringVisitableToLegacyString(stringVisitable: StringVisitable): String {
        val builder = StringBuilder()
        var lastStyle = Style.EMPTY
        stringVisitable.visit(
            { style, string ->
                if (lastStyle != style) {
                    builder.append(requiredStyleChangeString(lastStyle, style))
                    lastStyle = style
                }
                builder.append(string)
                Optional.empty<Any>()
            },
            Style.EMPTY,
        )
        return builder.toString()
    }

    @JvmStatic
    fun legacyStringToStringVisitable(legacyString: String): StringVisitable {
        val segments = mutableListOf<StringVisitable>()
        var lastStyle = Style.EMPTY

        val sb = StringBuilder()

        var index = 0
        while (index < legacyString.length) {
            val char = legacyString[index]

            if (char == '§') {

                if (index + 1 < legacyString.length) {

                    segments.add(StringVisitable.styled(sb.toString(), lastStyle))

                    sb.clear()

                    val nextChar = legacyString[index + 1]

                    if (legacyString[index + 1] == '#') {

                        if (index + 8 < legacyString.length) {
                            val hexColor = legacyString.substring(index + 2, index + 8)

                            hexColor.toIntOrNull(16) ?.let { color: Int ->
                                lastStyle = Style.EMPTY.withColor(color)
                                index += 7
                            }
                        }

                    } else {

                        val formatting = Formatting.byCode(nextChar)

                        if (formatting != null) {
                            lastStyle = lastStyle.withExclusiveFormatting(formatting)
                        } else if (nextChar == 'z') {
                            lastStyle = Style.EMPTY.withColor(CHROMA_COLOR)
                        }

                        index ++
                    }
                }

            } else {
                sb.append(char)
            }
            index ++
        }
        if (sb.isNotEmpty()) {
            segments.add(StringVisitable.styled(sb.toString(), lastStyle))
            sb.clear()
        }
        return StringVisitable.concat(segments)
    }

    @JvmStatic
    fun legacyTextToOrderedText(legacyString: String?): OrderedText {
        return OrderedText { visitor ->

            legacyString ?: return@OrderedText true

            var lastStyle = Style.EMPTY

            var index = 0

            while (index < legacyString.length) {
                val char = legacyString[index]

                if (char == '§') {

                    if (index + 1 < legacyString.length) {

                        val nextChar = legacyString[index + 1]

                        if (legacyString[index + 1] == '#') {

                            if (index + 8 < legacyString.length) {
                                val hexColor = legacyString.substring(index + 2, index + 8)

                                hexColor.toIntOrNull(16) ?.let { color: Int ->
                                    lastStyle = Style.EMPTY.withColor(color)
                                    index += 7
                                }
                            }

                        } else {

                            val formatting = Formatting.byCode(nextChar)

                            if (formatting != null) {
                                lastStyle = lastStyle.withExclusiveFormatting(formatting)
                            } else if (nextChar == 'z') {
                                lastStyle = lastStyle.withColor(CHROMA_COLOR)
                            }

                            index ++
                        }
                    }

                } else if (char.isHighSurrogate() && index + 1 < legacyString.length) {
                    val nextChar = legacyString[index + 1]

                    if (nextChar.isLowSurrogate()) {
                        visitor.accept(0, lastStyle, Character.toCodePoint(char, nextChar))

                        index ++
                    } else {
                        visitor.accept(0, lastStyle, char.code)
                    }
                } else {
                    visitor.accept(0, lastStyle, char.code)
                }
                index ++
            }
            true
        }
    }

    private fun requiredStyleChangeString(from: Style, to: Style, exclusive: Boolean = false): String {
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

        if ((from.color != to.color && to.color != null) || (reset && to.color != null)) {
            if (!exclusive) sb.append(Formatting.RESET.toString())

            if (to.color?.name == "chroma") {
                sb.append("§z")
            } else {

                val colorFormatting = to.color?.toChatFormatting()

                if (colorFormatting != null) {
                    sb.append(colorFormatting.toString())
                } else {
                    sb.append("§${to.color?.hexCode}")
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
