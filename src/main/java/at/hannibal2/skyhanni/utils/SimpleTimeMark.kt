package at.hannibal2.skyhanni.utils

import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.time.Instant
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

    fun isFarPast() = millis == 0L

    override fun compareTo(other: SimpleTimeMark): Int = millis.compareTo(other.millis)

    override fun toString(): String {
        if (millis == 0L) return "The Far Past"
        return Instant.ofEpochMilli(millis).toString()
    }

    fun toMillis() = millis

    fun toSkyBlockTime() = SkyBlockTime.fromInstant(Instant.ofEpochMilli(millis))

    companion object {
        fun now() = SimpleTimeMark(System.currentTimeMillis())
        fun farPast() = SimpleTimeMark(0)

        fun Long.asTimeMark() = SimpleTimeMark(this)
        fun SkyBlockTime.asTimeMark() = SimpleTimeMark(toMillis())
    }
}
