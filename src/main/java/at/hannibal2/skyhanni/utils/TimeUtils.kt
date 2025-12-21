package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.regex.Matcher
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Suppress("TooManyFunctions")
object TimeUtils {

    val isAprilFoolsDay: Boolean by RecalculatingValue(1.seconds) {
        val itsTime = LocalDate.now().let { it.month == Month.APRIL && it.dayOfMonth == 1 }
        val (always, never) = SkyHanniMod.feature.dev.debug.let { it.alwaysFunnyTime to it.neverFunnyTime }
        !never && (always || itsTime)
    }

    fun Duration.format(
        biggestUnit: TimeUnit = TimeUnit.YEAR,
        showMilliSeconds: Boolean = this.absoluteValue < 1.seconds,
        longName: Boolean = false,
        maxUnits: Int = -1,
        showSmallerUnits: Boolean = false,
        showNegativeAsSoon: Boolean = true,
    ): String {
        var millis = inWholeMilliseconds.absoluteValue
        val prefix = if (isNegative()) {
            if (showNegativeAsSoon) return "Soon"
            "-"
        } else ""
        val parts = mutableMapOf<TimeUnit, Int>()

        for (unit in TimeUnit.entries) {
            if (unit.ordinal >= biggestUnit.ordinal) {
                val factor = unit.factor
                parts[unit] = (millis / factor).toInt()
                millis %= factor
            }
        }

        val largestNonZeroUnit = parts.firstNotNullOfOrNull { if (it.value != 0) it.key else null } ?: TimeUnit.SECOND

        if (absoluteValue < 1.seconds) {
            val formattedMillis = (millis / 100).toInt()
            return "${prefix}0.${formattedMillis}${TimeUnit.SECOND.getName(formattedMillis, longName)}"
        }

        var currentUnits = 0
        val result = buildString {
            for ((unit, value) in parts) {
                val showUnit = value != 0 || (showSmallerUnits && unit.factor <= largestNonZeroUnit.factor)

                if (showUnit) {
                    val formatted = value.addSeparators()
                    val text = if (unit == TimeUnit.SECOND && showMilliSeconds) {
                        val formattedMillis = (millis / 100).toInt()
                        "$formatted.$formattedMillis"
                    } else formatted

                    val name = unit.getName(value, longName)
                    append("$text$name ")
                    if (maxUnits != -1 && ++currentUnits == maxUnits) break
                }
            }
        }
        return prefix + result.trim()
    }

    fun Duration.timerColor(default: String = "§f") = when (this) {
        in 0.seconds..60.seconds -> "§c"
        in 60.seconds..3.minutes -> "§6"
        in 3.minutes..10.minutes -> "§e"
        else -> default
    }

    fun Iterable<Duration>.average(): Duration {
        var sum: Duration = Duration.ZERO
        var count = 0
        for (element in this) {
            sum += element
            count++
        }
        return if (count == 0) Duration.ZERO else sum / count
    }

    val Duration.inWholeTicks: Int get() = (inWholeMilliseconds / 50).toInt()

    private fun String.preFixDurationString() =
        replace(Regex("(\\d+)([yMWwdhms])(?!\\s)"), "$1$2 ") // Add a space only after common time units
            .trim()

    fun getDuration(string: String): Duration =
        getDurationOrNull(string) ?: throw RuntimeException("Invalid format: '$string'")

    fun getDurationOrNull(string: String): Duration? = getMillis(string.preFixDurationString())

    private fun getMillis(string: String) = UtilsPatterns.timeAmountPattern.matchMatcher(string.lowercase().trim()) {
        years("y") + days("d") + hours("h") + minutes("m") + seconds("s")
    } ?: tryAlternativeFormat(string)

    private fun tryAlternativeFormat(string: String): Duration? {
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

            1 -> split[0].toInt() * 1000

            else -> return null
        }.milliseconds
    }

    fun SkyBlockTime.formatted(
        dayAndMonthElement: Boolean = true,
        yearElement: Boolean = true,
        hoursAndMinutesElement: Boolean = true,
        timeFormat24h: Boolean = false,
        exactMinutes: Boolean = true,
    ): String {
        val hour = if (timeFormat24h) this.hour else (this.hour + 11) % 12 + 1
        val timeOfDay = if (!timeFormat24h) {
            if (this.hour > 11) "pm" else "am"
        } else ""
        val minute = this.minute.let {
            if (exactMinutes) it else it - (it % 10)
        }.toString().padStart(2, '0')
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
        return ScoreboardData.tryToReplaceScoreboardLine(
            if (datePart.isNotEmpty() && timePart.isNotEmpty()) {
                "$datePart, $timePart"
            } else {
                "$datePart$timePart".trim()
            },
        )
    }

    fun getCurrentLocalDate(): LocalDate = LocalDate.now(ZoneId.of("UTC"))

    fun LocalDateTime.getCountdownFormat(): String {
        val timeNow = LocalDateTime.now()
        val yearDiff = year - timeNow.year
        val monthDiff = monthValue - timeNow.monthValue
        val dayDiff = dayOfMonth - timeNow.dayOfMonth

        return when {
            yearDiff == 0 && monthDiff == 0 && dayDiff == 0 -> "HH:mm:ss"
            (yearDiff == 0 && monthDiff == 0) || (yearDiff == 0) -> "MM-dd HH:mm"
            else -> "yyyy-MM-dd HH:mm"
        }
    }

    val Long.ticks get() = (this * 50).milliseconds
    val Int.ticks get() = (this * 50).milliseconds

    val Float.minutes get() = toDouble().minutes

    // TODO move into lorenz logger. then rewrite lorenz logger and use something different entirely
    fun SimpleDateFormat.formatCurrentTime(): String = this.format(System.currentTimeMillis())

    fun getTablistEndTime(string: String, currentCooldownEnd: SimpleTimeMark?): SimpleTimeMark? =
        UtilsPatterns.timeAmountPattern.matchMatcher(string.lowercase().trim()) {
            val years = years("y")
            val days = days("d")
            val hours = hours("h")
            val minutes = minutesOrNull("m")
            val seconds = secondsOrNull("s")

            // we don't care about precision with these values
            val largerDuration = years + days + hours
            if (minutes == null && seconds == null) {
                return if (largerDuration == 0.seconds) {
                    // assume passed string was invalid
                    null
                } else {
                    SimpleTimeMark.now() + largerDuration
                }
            }

            val tabCooldownEnd = SimpleTimeMark.now() + (minutes ?: 0.seconds) + (seconds ?: 0.seconds)
            return if (shouldSetCooldown(tabCooldownEnd, currentCooldownEnd, seconds)) {
                if (seconds == null) {
                    // tablist always rounds down, so we'll assume it just updated and add a minute
                    tabCooldownEnd + 1.minutes + largerDuration
                } else tabCooldownEnd + largerDuration
            } else currentCooldownEnd
        }

    private fun shouldSetCooldown(tabCooldownEnd: SimpleTimeMark, currentCooldownEnd: SimpleTimeMark?, seconds: Duration?): Boolean = when {
        currentCooldownEnd == null -> true
        // tablist can have up to 6 seconds of delay, besides this, there is no scenario where tablist will overestimate cooldown
        tabCooldownEnd > ((currentCooldownEnd) + 6.seconds) -> true
        // tablist sometimes rounds down to nearest min
        (tabCooldownEnd + 1.minutes) < (currentCooldownEnd) && seconds == null -> true
        // tablist shouldn't underestimate if it is displaying seconds
        (tabCooldownEnd + 1.seconds) < (currentCooldownEnd) && seconds != null -> true
        else -> false
    }

    private fun Matcher.years(string: String) = groupOrNull(string)?.toLong()?.years ?: 0.seconds
    private fun Matcher.days(string: String) = groupOrNull(string)?.toLong()?.days ?: 0.seconds
    private fun Matcher.hours(string: String) = groupOrNull(string)?.toLong()?.hours ?: 0.seconds
    private fun Matcher.minutesOrNull(string: String) = groupOrNull(string)?.toLong()?.minutes
    private fun Matcher.minutes(string: String) = minutesOrNull(string) ?: 0.seconds
    private fun Matcher.secondsOrNull(string: String) = groupOrNull(string)?.toLong()?.seconds
    private fun Matcher.seconds(string: String) = secondsOrNull(string) ?: 0.seconds
}

private const val FACTOR_SECONDS = 1000L
private const val FACTOR_MINUTES = FACTOR_SECONDS * 60
private const val FACTOR_HOURS = FACTOR_MINUTES * 60
private const val FACTOR_DAYS = FACTOR_HOURS * 24
private const val FACTOR_YEARS = (FACTOR_DAYS * 365.25).toLong()

enum class TimeUnit(val factor: Long, private val shortName: String, private val longName: String) {
    YEAR(FACTOR_YEARS, "y", "Year"),
    DAY(FACTOR_DAYS, "d", "Day"),
    HOUR(FACTOR_HOURS, "h", "Hour"),
    MINUTE(FACTOR_MINUTES, "m", "Minute"),
    SECOND(FACTOR_SECONDS, "s", "Second"),
    ;

    fun asDuration(value: Int): Duration = (value * factor).milliseconds

    fun getName(value: Int, longFormat: Boolean) = if (longFormat) {
        " $longName" + if (value == 1) "" else "s"
    } else shortName

    fun format(value: Int, longFormat: Boolean = false) = value.addSeparators() + getName(value, longFormat)

    companion object {
        fun getByName(name: String, tryRemoveSuffix: Boolean = true) = entries.firstOrNull {
            val nameMatches = it.name.equals(name, ignoreCase = true)
            if (nameMatches) return@firstOrNull true
            if (!tryRemoveSuffix) return@firstOrNull false
            val suffixRemoved = name.uppercase().removeSuffix("S")
            it.name.equals(suffixRemoved, ignoreCase = true)
        }
    }
}

val Duration.inPartialSeconds: Double get() = toDouble(DurationUnit.SECONDS)
val Duration.inPartialMinutes: Double get() = inPartialSeconds / 60
val Duration.inPartialHours: Double get() = inPartialSeconds / 3600
val Duration.inPartialDays: Double get() = inPartialSeconds / 86_400
val Duration.inPartialYears: Double get() = inPartialSeconds / (86_400 * 365.25)
val Long.years: Duration get() = this.times(365.25).days
