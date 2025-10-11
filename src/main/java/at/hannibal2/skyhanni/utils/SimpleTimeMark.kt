package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@JvmInline
value class SimpleTimeMark(private val millis: Long) : Comparable<SimpleTimeMark> {

    operator fun minus(other: SimpleTimeMark) =
        (millis - other.millis).milliseconds

    operator fun plus(other: Duration) =
        SimpleTimeMark(millis + other.inWholeMilliseconds)

    operator fun minus(other: Duration) = plus(-other)

    fun passedSince() = now() - this

    fun timeUntil() = -passedSince()

    fun isInPast() = timeUntil().isNegative()

    fun isInFuture() = timeUntil().isPositive()

    fun isFarPast() = millis == FAR_PAST_MS

    fun isFarFuture() = millis == FAR_FUTURE_MS

    fun takeIfInitialized() = if (isFarPast() || isFarFuture()) null else this

    fun absoluteDifference(other: SimpleTimeMark) = abs(millis - other.millis).milliseconds

    override fun compareTo(other: SimpleTimeMark): Int = millis.compareTo(other.millis)

    override fun toString(): String = when (this) {
        farPast() -> "The Far Past"
        farFuture() -> "The Far Future"
        else -> Instant.ofEpochMilli(millis).toString()
    }

    private fun String.applyTimeFormat(): String {
        return if (SkyHanniMod.feature.gui.timeFormat24h) {
            replace("h", "H").replace("a", "")
        } else this
    }

    fun formattedDate(pattern: String): String {
        val newPattern = pattern.applyTimeFormat()
        val instant = Instant.ofEpochMilli(millis)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern(newPattern.trim())
        return localDateTime.format(formatter)
    }

    fun toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    fun toMillis() = millis

    fun toSkyBlockTime(): SkyBlockTime = SkyBlockTime.fromTimeMark(this)

    fun toLocalDate(): LocalDate = toLocalDateTime().toLocalDate()

    companion object {

        fun now() = SimpleTimeMark(System.currentTimeMillis())

        private const val FAR_PAST_MS = 0L
        private const val FAR_FUTURE_MS = Long.MAX_VALUE

        private val FAR_PAST = SimpleTimeMark(FAR_PAST_MS)
        private val FAR_FUTURE = SimpleTimeMark(FAR_FUTURE_MS)

        @JvmStatic
        @JvmName("farPast")
        fun farPast() = FAR_PAST
        fun farFuture() = FAR_FUTURE

        fun Duration.fromNow() = now() + this

        fun Long.asTimeMark() = SimpleTimeMark(this)
    }
}
