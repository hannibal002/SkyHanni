package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.Stopwatch
import com.google.gson.annotations.Expose
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackerData {
    @Expose
    private var sessionUptime: Map<SessionUptime, Stopwatch> = mapOf(
        Pair(SessionUptime.Normal(NormalSession.NORMAL), Stopwatch())
    )

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

    fun reset() {
        for (session in sessionUptime.entries) {
            sessionUptime[session.key]?.reset()
        }
        resetData()
    }

    protected abstract fun resetData()
}

sealed class SessionUptime {
    data class Normal(val sessionType: NormalSession) : SessionUptime()
    data class Garden(val sessionType: GardenSession) : SessionUptime()
}

enum class NormalSession {
    NORMAL,
}

enum class GardenSession {
    PEST,
    VISITOR,
    CROP,
}


