package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.GuiRenderUtils.darkenColor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiUtilRenderComponents
import net.minecraft.util.ChatComponentText
import org.intellij.lang.annotations.Language
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object StringUtils {
    fun String.firstLetterUppercase(): String {
        if (isEmpty()) return this

        val lowercase = lowercase()
        val first = lowercase[0].uppercase()
        return first + lowercase.substring(1)
    }

    fun String.removeColor(): String {
//        return replace("(?i)\\u00A7.", "")

        val builder = StringBuilder()
        var skipNext = false
        for (c in this.toCharArray()) {
            if (c == '§') {
                skipNext = true
                continue
            }
            if (skipNext) {
                skipNext = false
                continue
            }
            builder.append(c)
        }

        return builder.toString()
    }

    /**
     * From https://stackoverflow.com/questions/10711494/get-values-in-treemap-whose-string-keys-start-with-a-pattern
     */
    fun <T> subMapWithKeysThatAreSuffixes(prefix: String, map: NavigableMap<String?, T>): Map<String?, T>? {
        if ("" == prefix) return map
        val lastKey = nextLexicographicallyStringWithSameLength(prefix)
        return map.subMap(prefix, true, lastKey, false)
    }

    fun nextLexicographicallyStringWithSameLength(input: String): String {
        val lastCharPosition = input.length - 1
        val inputWithoutLastChar = input.substring(0, lastCharPosition)
        val lastChar = input[lastCharPosition]
        val incrementedLastChar = (lastChar.code + 1).toChar()
        return inputWithoutLastChar + incrementedLastChar
    }

    fun UUID.toDashlessUUID(): String {
        return toString().replace("-", "")
    }

    fun String.matchRegex(@Language("RegExp") regex: String): Boolean = regex.toRegex().matches(this)

    private fun String.removeAtBeginning(text: String): String =
        if (this.startsWith(text)) substring(text.length) else this

    // TODO find better name for this method
    inline fun <T> Pattern.matchMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.matches()) consumer(it) else null }

    fun String.cleanPlayerName(): String {
        val split = split(" ")
        return if (split.size > 1) {
            split[1].removeColor()
        } else {
            split[0].removeColor()
        }
    }

    fun getColor(string: String, default: Int, darker: Boolean = true): Int {
        val stringPattern = "§[0123456789abcdef].*".toPattern()

        val matcher = stringPattern.matcher(string)
        if (matcher.matches()) {
            val colorInt = Minecraft.getMinecraft().fontRendererObj.getColorCode(string[1])
            return if (darker) {
                colorInt.darkenColor()
            } else {
                "ff${Integer.toHexString(colorInt)}".toLong(radix = 16).toInt()
            }
        }
        return default
    }

    fun encodeBase64(input: String) = Base64.getEncoder().encodeToString(input.toByteArray())

    fun decodeBase64(input: String) = Base64.getDecoder().decode(input).decodeToString()

    fun addFormat(text: String, format: String): String {
        if (text.length < 2) return text

        val rawText = text.substring(2)
        return if (rawText == text.removeColor()) {
            val originalColor = text.substring(0, 2)
            "$originalColor$format$rawText"
        } else {
            "$format$text"
        }
    }


    fun String.removeWordsAtEnd(i: Int) = split(" ").dropLast(i).joinToString(" ")

    fun String.splitLines(width: Int): String {
        val fr = Minecraft.getMinecraft().fontRendererObj
        return GuiUtilRenderComponents.splitText(
            ChatComponentText(this), width, fr, false, false
        ).joinToString("\n") {
            val text = it.formattedText
            val formatCode = Regex("(?:§[a-f0-9l-or]|\\s)*")
            formatCode.matchAt(text, 0)?.let { matcher ->
                val codes = matcher.value.replace("\\s".toRegex(), "")
                codes + text.removeRange(matcher.range)
            } ?: text
        }
    }

    fun optionalPlural(number: Int, singular: String, plural: String) =
        "$number " + if (number == 1) singular else plural

    fun progressBar(percentage: Double, steps: Int = 25): Any {
        //'§5§o§2§l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §f§l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §l§m §r §e348,144.3§6/§e936k'
        val prefix = "§5§o§2"
        val step = "§l§m "
        val missing = "§f"
        val end = "§r"

        val builder = StringBuilder()
        var inMissingArea = false
        builder.append(prefix)
        for (i in 0..steps) {
            val toDouble = i.toDouble()
            val stepPercentage = toDouble / steps
            if (stepPercentage >= percentage) {
                if (!inMissingArea) {
                    builder.append(missing)
                    inMissingArea = true
                }
            }
            builder.append(step)
        }
        builder.append(end)
        return builder.toString()
    }

    fun String.capAtMinecraftLength(limit: Int) =
        capAtLength(limit) { Minecraft.getMinecraft().fontRendererObj.getCharWidth(it) }

    private fun String.capAtLength(limit: Int, lengthJudger: (Char) -> Int): String {
        var i = 0
        return takeWhile {
            i += lengthJudger(it)
            i < limit
        }
    }
}