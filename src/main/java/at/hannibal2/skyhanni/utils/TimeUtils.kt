package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.mixins.hooks.tryToReplaceScoreboardLine
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object TimeUtils {

    fun Duration.format(
        biggestUnit: TimeUnit = TimeUnit.YEAR,
        showMilliSeconds: Boolean = false,
        longName: Boolean = false,
        maxUnits: Int = -1,
    ): String {
        var millis = inWholeMilliseconds
        val parts = mutableMapOf<TimeUnit, Int>()

        for (unit in TimeUnit.entries) {
            if (unit.ordinal >= biggestUnit.ordinal) {
                val factor = unit.factor
                parts[unit] = (millis / factor).toInt()
                millis %= factor
            }
        }

        var currentUnits = 0
        val result = buildString {
            for ((unit, value) in parts) {
                if (value != 0) {
                    val formatted = unit.format(value, longName)
                    append(formatted)
                    if (unit == TimeUnit.SECOND && showMilliSeconds) {
                        val formattedMillis = (millis / 100).toInt()
                        append(".$formattedMillis")
                    }

                    append(" ")
                    if (maxUnits != -1 && ++currentUnits == maxUnits) break
                }
            }
        }
        return result.trim()
    }

    fun Duration.timerColor(default: String = "§f") = when (this) {
        in 0.seconds..60.seconds -> "§c"
        in 60.seconds..3.minutes -> "§6"
        in 3.minutes..10.minutes -> "§e"
        else -> default
    }

    val Duration.inWholeTicks: Int
        get() = (inWholeMilliseconds / 50).toInt()

    fun getDuration(string: String) = getMillis(string.replace("m", "m ").replace("  ", " ").trim())

    private fun getMillis(string: String) = UtilsPatterns.timeAmountPattern.matchMatcher(string.lowercase().trim()) {
        val years = group("y")?.toLong() ?: 0L
        val days = group("d")?.toLong() ?: 0L
        val hours = group("h")?.toLong() ?: 0L
        val minutes = group("m")?.toLong() ?: 0L
        val seconds = group("s")?.toLong() ?: 0L

        var millis = 0L
        millis += seconds * 1000
        millis += minutes * 60 * 1000
        millis += hours * 60 * 60 * 1000
        millis += days * 24 * 60 * 60 * 1000
        millis += (years * 365.25 * 24 * 60 * 60 * 1000).toLong()

        millis.toDuration(DurationUnit.MILLISECONDS)
    } ?: tryAlternativeFormat(string)

    private fun tryAlternativeFormat(string: String): Duration {
        val split = string.split(":")
        return when (split.size) {
            3 -> {
                val hours = split[0].toInt() * 1000 * 60 * 60
                val minutes = split[1].toInt() * 1000 * 60
                val seconds = split[2].toInt() * 1000
                seconds + minutes + hours
            }

            2 -> {
                val minutes = split[0].toInt() * 1000 * 60
                val seconds = split[1].toInt() * 1000
                seconds + minutes
            }

            1 -> {
                split[0].toInt() * 1000
            }

            else -> {
                throw RuntimeException("Invalid format: '$string'")
            }
        }.milliseconds
    }

    fun SkyBlockTime.formatted(
        dayAndMonthElement: Boolean = true,
        yearElement: Boolean = true,
        hoursAndMinutesElement: Boolean = true
    ): String {
        val hour = (this.hour + 11) % 12 + 1
        val timeOfDay = if (this.hour > 11) "pm" else "am"
        val minute = this.minute.toString().padStart(2, '0')
        val month = SkyBlockTime.monthName(this.month)
        val day = this.day
        val daySuffix = SkyBlockTime.daySuffix(day)
        val year = this.year

        val datePart = when {
            yearElement -> "$month $day$daySuffix, Year $year"
            dayAndMonthElement -> "$month $day$daySuffix"
            else -> ""
        }
        val timePart = if (hoursAndMinutesElement) "$hour:$minute$timeOfDay" else ""

        /**
         * We replace the line here, because the user might want color month names
         */
        return tryToReplaceScoreboardLine(
            if (datePart.isNotEmpty() && timePart.isNotEmpty()) {
                "$datePart, $timePart"
            } else {
                "$datePart$timePart".trim()
            }
        ) ?: ""
    }

    fun getCurrentLocalDate(): LocalDate = LocalDate.now(ZoneId.of("UTC"))

    val Long.ticks get() = (this * 50).milliseconds
    val Int.ticks get() = (this * 50).milliseconds

    val Float.minutes get() = toDouble().minutes
}

private const val FACTOR_SECONDS = 1000L
private const val FACTOR_MINUTES = FACTOR_SECONDS * 60
private const val FACTOR_HOURS = FACTOR_MINUTES * 60
private const val FACTOR_DAYS = FACTOR_HOURS * 24
private const val FACTOR_YEARS = (FACTOR_DAYS * 365.25).toLong()

enum class TimeUnit(val factor: Long, val shortName: String, val longName: String) {
    YEAR(FACTOR_YEARS, "y", "Year"),
    DAY(FACTOR_DAYS, "d", "Day"),
    HOUR(FACTOR_HOURS, "h", "Hour"),
    MINUTE(FACTOR_MINUTES, "m", "Minute"),
    SECOND(FACTOR_SECONDS, "s", "Second"),
    ;

    fun format(value: Int, longFormat: Boolean = false) = if (longFormat) {
        "${value.addSeparators()} $longName" + if (value > 1) "s" else ""
    } else {
        "${value.addSeparators()}$shortName"
    }
}
