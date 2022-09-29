package at.hannibal2.skyhanni.utils

import java.text.DecimalFormat
import java.util.*

object StringUtils {

    private val durationFormat = DecimalFormat("00")

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

    fun formatDuration(seconds: Long): String {
        var sec: Long = seconds

        var minutes: Long = sec / 60
        sec %= 60

        var hours = minutes / 60
        minutes %= 60

        val days = hours / 24
        hours %= 24


        val formatHours = durationFormat.format(hours)
        val formatMinutes = durationFormat.format(minutes)
        val formatSeconds = durationFormat.format(sec)

        if (days > 0) {
            return "${days}d $formatHours:$formatMinutes:$formatSeconds"
        }
        if (hours > 0) {
            return "$formatHours:$formatMinutes:$formatSeconds".removeAtBeginning("0")
        }
        if (minutes > 0) {
            return "$formatMinutes:$formatSeconds".removeAtBeginning("0")
        }
        if (sec > 0) {
            return "${sec}s"
        }

        return "Now"
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

}

private fun String.removeAtBeginning(text: String): String = if (this.startsWith(text)) substring(text.length) else this
