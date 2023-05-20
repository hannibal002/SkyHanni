package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import org.intellij.lang.annotations.Language
import java.awt.Color
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object StringUtils {
    fun String.firstLetterUppercase(): String {
        if (isEmpty()) return this

        val lowercase = this.lowercase()
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
        val lastKey = createLexicographicallyNextStringOfTheSameLength(prefix)
        return map.subMap(prefix, true, lastKey, false)
    }

    fun createLexicographicallyNextStringOfTheSameLength(input: String): String {
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
                darkenInt(colorInt)
            } else {
                "ff${Integer.toHexString(colorInt)}".toLong(radix = 16).toInt()
            }
        }
        return default
    }

    // does not work
    private fun darkenInt(int: Int): Int {
        return "ff${String.format("%06x", Color(Integer.toHexString(int).toLong(radix = 16).toInt()).darker().rgb)}".toLong(radix = 16).toInt()
    }
}
