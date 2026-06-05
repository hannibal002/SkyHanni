package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant as KInstant

/**
 * This is a Helper Class similar to [SimpleTimeMark], but for a rough estimate of Server Ticks instead of real time.
 *
 * This can provide a more accurate estimate of certain timers for ingame events, because some are based off of
 * the server's tps instead of real time, and therefore are affected by server lag.
 */
@JvmInline
value class ServerTimeMark internal constructor(private val millis: Long) : Comparable<ServerTimeMark> {

    operator fun minus(other: ServerTimeMark) =
        (millis - other.millis).milliseconds

    operator fun plus(other: Duration) =
        ServerTimeMark(millis + other.inWholeMilliseconds)

    operator fun minus(other: Duration) = plus(-other)

    fun passedSince(): Duration = now() - this

    fun timeUntil(): Duration = -passedSince()

    fun isInPast(): Boolean = timeUntil().isNegative()

    fun isInFuture(): Boolean = timeUntil().isPositive()

    fun isFarPast() = millis <= FAR_PAST_MS

    fun isFarFuture() = millis == FAR_FUTURE_MS

    fun takeIfInitialized() = if (isFarPast() || isFarFuture()) null else this

    fun absoluteDifference(other: ServerTimeMark) = abs(millis - other.millis).ticks

    override fun compareTo(other: ServerTimeMark): Int = millis.compareTo(other.millis)

    fun toMillis() = millis

    override fun toString(): String = when (millis) {
        FAR_PAST_MS -> "The Far Past"
        FAR_FUTURE_MS -> "The Far Future"
        else -> "ServerTimeMark(millis=$millis})"
    }

    companion object {
        fun now() = ServerTimeMark(MinecraftData.totalServerTicks * 50L)

        // A sentinel value that is above Long.MIN_VALUE, so we don't worry about underflow
        // If this causes problems replace with `Long.MIN_VALUE / 2`
        @OptIn(ExperimentalTime::class)
        private val FAR_PAST_MS = KInstant.DISTANT_PAST.toEpochMilliseconds()
        private const val FAR_FUTURE_MS = Long.MAX_VALUE

        private val FAR_PAST = ServerTimeMark(FAR_PAST_MS)
        private val FAR_FUTURE = ServerTimeMark(FAR_FUTURE_MS)

        @JvmStatic
        @JvmName("farPast")
        fun farPast() = FAR_PAST
        fun farFuture() = FAR_FUTURE

        fun Duration.fromServerNow() = now() + this
    }
}
