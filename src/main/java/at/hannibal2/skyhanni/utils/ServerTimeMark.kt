package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.MinecraftData
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * This is a Helper Class similar to [SimpleTimeMark], but for a rough estimate of Server Ticks instead of real time.
 *
 * This can provide a more accurate estimate of certain timers for ingame events, because some are based off of
 * the server's tps instead of real time, and therefore are affected by server lag.
 */
@JvmInline
value class ServerTimeMark internal constructor(private val millis: Long) : Comparable<ServerTimeMark> {

    operator fun minus(other: ServerTimeMark): Duration =
        (millis - other.millis).milliseconds

    operator fun plus(other: Duration) =
        ServerTimeMark(millis + other.inWholeMilliseconds)

    operator fun minus(other: Duration) = plus(-other)

    fun passedSince(): Duration = now() - this

    fun timeUntil(): Duration = -passedSince()

    fun passedSinceSmooth(): Duration = nowSmooth() - this

    fun timeUntilSmooth(): Duration = -passedSinceSmooth()

    fun isInPast(): Boolean = timeUntil().isNegative()

    fun isInFuture(): Boolean = timeUntil().isPositive()

    fun isFarPast() = millis == FAR_PAST_MS

    fun isFarFuture() = millis == FAR_FUTURE_MS

    fun takeIfInitialized() = if (isFarPast() || isFarFuture()) null else this

    fun absoluteDifference(other: ServerTimeMark) = abs(millis - other.millis).milliseconds

    override fun compareTo(other: ServerTimeMark): Int = millis.compareTo(other.millis)

    override fun toString(): String = when (millis) {
        FAR_PAST_MS -> "The Far Past"
        FAR_FUTURE_MS -> "The Far Future"
        else -> "ServerTimeMark(millis=$millis, now=${MinecraftData.totalServerTicks})"
    }

    fun toMillis() = millis

    companion object {
        // This could technically be any large number,
        // but this way is better for compatibility with `SimpleTimeMark`
        private val startTime = SimpleTimeMark.now().toMillis()

        fun now(): ServerTimeMark = ServerTimeMark(startTime + MinecraftData.totalServerTicks * TICK_DURATION_MS.inWholeMilliseconds)

        /**
         * Smooth time for UI purposes only.
         * Don't use for gameplay logic.
         */
        fun nowSmooth(): ServerTimeMark {
            // Ensure that the time doesn't jump forward more than 50ms to the next tick
            val delta = MinecraftData.lastPingMs.passedSince().coerceAtMost(TICK_DURATION_MS)
            return now().plus(delta)
        }

        private const val FAR_PAST_MS = 0L
        private const val FAR_FUTURE_MS = Long.MAX_VALUE
        private val TICK_DURATION_MS = 50.toDuration(DurationUnit.MILLISECONDS)

        private val FAR_PAST = ServerTimeMark(FAR_PAST_MS)
        private val FAR_FUTURE = ServerTimeMark(FAR_FUTURE_MS)

        @JvmStatic
        @JvmName("farPast")
        fun farPast() = FAR_PAST
        fun farFuture() = FAR_FUTURE

        fun Duration.fromServerNow() = now() + this
    }
}
