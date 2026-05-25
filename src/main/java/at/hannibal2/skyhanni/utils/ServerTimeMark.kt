package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.MinecraftData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    operator fun minus(other: Duration): ServerTimeMark = plus(-other)

    fun passedSince(): Duration = now() - this

    fun timeUntil(): Duration = -passedSince()

    fun isInPast(): Boolean = timeUntil().isNegative()

    fun isInFuture(): Boolean = timeUntil().isPositive()

    fun isFarPast(): Boolean = this == FAR_PAST

    fun isFarFuture(): Boolean = millis == FAR_FUTURE_MS

    fun passedSinceSmooth(): Duration = nowSmooth() - this

    fun timeUntilSmooth(): Duration = -passedSinceSmooth()

    override fun compareTo(other: ServerTimeMark): Int = millis.compareTo(other.millis)

    override fun toString(): String = when (millis) {
        FAR_PAST_MS -> "The Far Past"
        FAR_FUTURE_MS -> "The Far Future"
        else -> "ServerTimeMark(millis=$millis, now=${MinecraftData.totalServerTicks})"
    }

    companion object {
        // This is done to be as compatible as possible with `SimpleTimeMark`,
        // This could technically just start at 1 million,
        // but this way is better parity
        private val startTime = System.currentTimeMillis()

        fun now(): ServerTimeMark {
            return ServerTimeMark(
                startTime + MinecraftData.totalServerTicks * 50L
            )
        }

        private var lastTickMs: Long = System.currentTimeMillis()

        fun onServerTick() {
            lastTickMs = System.currentTimeMillis()
        }

        /**
         * Smooth time for UI purposes only.
         * Don't use for gameplay logic.
         */
        fun nowSmooth(): ServerTimeMark {
            val base = MinecraftData.totalServerTicks * 50L
            // Ensure that the time doesn't jump forward more than 50ms to the next tick
            val delta = (System.currentTimeMillis() - lastTickMs).coerceAtMost(50)
            return ServerTimeMark(startTime + base + delta)
        }

        private const val FAR_PAST_MS = 0L
        private const val FAR_FUTURE_MS = Long.MAX_VALUE

        private val FAR_PAST = ServerTimeMark(FAR_PAST_MS)
        private val FAR_FUTURE = ServerTimeMark(FAR_FUTURE_MS)

        fun farPast() = FAR_PAST
        fun farFuture() = FAR_FUTURE

        fun Duration.fromServerNow() = now() + this
    }
}
