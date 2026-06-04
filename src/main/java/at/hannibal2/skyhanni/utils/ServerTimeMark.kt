package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.utils.TimeUtils.inWholeTicks
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * This is a Helper Class similar to [SimpleTimeMark], but for a rough estimate of Server Ticks instead of real time.
 *
 * This can provide a more accurate estimate of certain timers for ingame events, because some are based off of
 * the server's tps instead of real time, and therefore are affected by server lag.
 */
@JvmInline
value class ServerTimeMark internal constructor(private val ticks: Int) : Comparable<ServerTimeMark> {

    operator fun minus(other: ServerTimeMark) =
        (ticks - other.ticks).ticks

    operator fun plus(other: Duration) =
        ServerTimeMark(ticks + other.inWholeTicks)

    operator fun minus(other: Duration) = plus(-other)

    fun passedSince(): Duration = now() - this

    fun timeUntil(): Duration = -passedSince()

    fun isInPast(): Boolean = timeUntil().isNegative()

    fun isInFuture(): Boolean = timeUntil().isPositive()

    fun isFarPast() = ticks == FAR_PAST_TICKS

    fun isFarFuture() = ticks == FAR_FUTURE_TICKS

    fun takeIfInitialized() = if (isFarPast() || isFarFuture()) null else this

    fun absoluteDifference(other: ServerTimeMark) = abs(ticks - other.ticks).ticks

    override fun compareTo(other: ServerTimeMark): Int = ticks.compareTo(other.ticks)

    override fun toString(): String = when (ticks) {
        FAR_PAST_TICKS -> "The Far Past"
        FAR_FUTURE_TICKS -> "The Far Future"
        else -> "ServerTimeMark(ticks=$ticks, now=${MinecraftData.totalServerTicks})"
    }

    companion object {
        // This is used to ensure no values are close to FAR_PAST_MS (which is 0)
        // This number is arbitrary, but I believe that 621 days worth of ticks is probably enough to ensure no overflow.
        private const val START_TICKS = Int.MAX_VALUE / 2

        fun now() = ServerTimeMark(START_TICKS + MinecraftData.totalServerTicks)

        private const val FAR_PAST_TICKS = 0
        private const val FAR_FUTURE_TICKS = Int.MAX_VALUE

        private val FAR_PAST = ServerTimeMark(FAR_PAST_TICKS)
        private val FAR_FUTURE = ServerTimeMark(FAR_FUTURE_TICKS)

        @JvmStatic
        @JvmName("farPast")
        fun farPast() = FAR_PAST
        fun farFuture() = FAR_FUTURE

        fun Duration.fromServerNow() = now() + this
    }
}
