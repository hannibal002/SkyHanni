package at.hannibal2.skyhanni.utils

import java.time.Instant

data class SkyBlockTime(
    val year: Int = 1,
    val month: Int = 1,
    val day: Int = 1,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
) {

    val monthName get() = monthName(month)
    val dayName get() = "$day${daySuffix(day)}"


    fun toInstant(): Instant? {
        return Instant.ofEpochMilli(toMillis())
    }

    fun toMillis(): Long {
        val skyBlockYear = 124 * 60 * 60.0
        val skyBlockMonth = skyBlockYear / 12
        val skyBlockDay = skyBlockMonth / 31
        val skyBlockHour = skyBlockDay / 24
        val skyBlockMinute = skyBlockHour / 60
        val skyBlockSecond = skyBlockMinute / 60

        var time = 0.0
        time += year * skyBlockYear
        time += (month - 1) * skyBlockMonth
        time += (day - 1) * skyBlockDay
        time += hour * skyBlockHour
        time += minute * skyBlockMinute
        time += second * skyBlockSecond
        time += 1559829300
        return time.toLong() * 1000
    }

    companion object {
        fun fromInstant(instant: Instant): SkyBlockTime {
            val skyBlockTimeZero = 1559829300000 // Day 1, Year 1
            var realMillis = (instant.toEpochMilli() - skyBlockTimeZero)

            val skyBlockYear = 124 * 60 * 60 * 1000
            val skyBlockMonth = skyBlockYear / 12
            val skyBlockDay = skyBlockMonth / 31
            val skyBlockHour = skyBlockDay / 24
            val skyBlockMinute = skyBlockHour / 60
            val skyBlockSecond = skyBlockMinute / 60

            fun getUnit(factor: Int): Int {
                val result = realMillis / factor
                realMillis %= factor
                return result.toInt()
            }

            val year = getUnit(skyBlockYear)
            val month = getUnit(skyBlockMonth) + 1
            val day = getUnit(skyBlockDay) + 1
            val hour = getUnit(skyBlockHour)
            val minute = getUnit(skyBlockMinute)
            val second = getUnit(skyBlockSecond)
            return SkyBlockTime(year, month, day, hour, minute, second)

        }

        fun now(): SkyBlockTime {
            return fromInstant(Instant.now())
        }

        fun monthName(month: Int): String {
            val prefix = when ((month - 1) % 3) {
                0 -> "Early "
                1 -> ""
                2 -> "Late "
                else -> "Undefined!"
            }

            val name = when ((month - 1) / 3) {
                0 -> "Spring"
                1 -> "Summer"
                2 -> "Autumn"
                3 -> "Winter"
                else -> "lol"
            }

            return prefix + name
        }

        fun daySuffix(n: Int): String {
            return if (n in 11..13) {
                "th"
            } else when (n % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
    }
}
