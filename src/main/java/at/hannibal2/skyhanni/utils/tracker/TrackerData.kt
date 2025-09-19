package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.Stopwatch
import com.google.gson.annotations.Expose
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackerData<T : SessionUptime>(
    private val uptimeClass: KClass<T>
) {
    @Expose
    private val sessionUptime: MutableMap<SessionUptime, Stopwatch> = mutableMapOf()

    private var activeSession: SessionUptime? = null

    init {
        addSessionUptime()
        activeSession = sessionUptime.keys.firstOrNull()
    }
    private fun addSessionUptime() {
        when (uptimeClass) {
            SessionUptime.Normal::class -> {
                NormalSession.entries.forEach { session ->
                    sessionUptime[SessionUptime.Normal(session)] = Stopwatch()
                }
            }

            SessionUptime.Garden::class -> {
                GardenSession.entries.forEach { session ->
                    sessionUptime[SessionUptime.Garden(session)] = Stopwatch()
                }
            }
        }
    }

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

