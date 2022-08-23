package at.hannibal2.skyhanni.utils

import java.text.DecimalFormat

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
            return "${days}d $formatHours:$formatMinutes:$formatSeconds ago"
        }
        if (hours > 0) {
            return "$formatHours:$formatMinutes:$formatSeconds ago"
        }
        if (minutes > 0) {
            return "$formatMinutes:$formatSeconds ago"
        }
        if (sec > 0) {
            return "${sec}s ago"
        }

        return "Now"
    }
}