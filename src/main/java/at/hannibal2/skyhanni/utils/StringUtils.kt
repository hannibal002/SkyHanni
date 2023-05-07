package at.hannibal2.skyhanni.utils

import org.intellij.lang.annotations.Language
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

    private fun String.removeAtBeginning(text: String): String = if (this.startsWith(text)) substring(text.length) else this

    // TODO find better name for this method No
    inline fun <T> Pattern.matchMatcher(text: String, consumer: Matcher.() -> T) =
        matcher(text).let { if (it.matches()) consumer(it) else null }
}
