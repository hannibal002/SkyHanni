package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.Stopwatch
import com.google.gson.annotations.Expose
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackerData : Resettable {
    @Expose
    private val sessionUptime: Map<SessionUptime, Stopwatch> = mapOf(
        Pair(SessionUptime.Normal(NormalSession.NORMAL), Stopwatch())
    )

    override fun reset() {
        super.reset()
        sessionUptime.values.forEach { it.reset() }
    }

    private var activeSession: SessionUptime? = sessionUptime.keys.firstOrNull()

    fun getActiveStopwatch(): Stopwatch? = activeSession?.let { sessionUptime[it] }

    fun setActiveStopwatch(session: SessionUptime) {
        if (session != activeSession) {
            val duration = sessionUptime[activeSession]?.pause(revertLap = true)
            activeSession = session
            sessionUptime[activeSession]?.add(duration ?: 0.seconds)
        }
        sessionUptime[activeSession]?.start(true)
    }

    open fun getTotalUptime(): Duration =
        sessionUptime.values.fold(Duration.ZERO) { acc, stopwatch ->
            acc + stopwatch.getDuration()
        }
}

