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

    fun getSessionMap() = sessionUptime.toMap()

    fun getActiveStopwatch(): Stopwatch? {
        val active = activeSession
        return active?.let { sessionUptime.getOrPut(active) { Stopwatch() } } ?: run {
            activeSession = sessionUptime.keys.firstOrNull()
            sessionUptime.getOrPut(activeSession ?: return null) { Stopwatch() }
        }
    }

    fun setActiveStopwatch(session: SessionUptime, swapExtraTime: Boolean) {
        if (session != activeSession) {
            val duration = getActiveStopwatch()?.pause(revertLap = swapExtraTime)
            activeSession = session
            if(swapExtraTime) getActiveStopwatch()?.add(duration ?: 0.seconds)
        }
        getActiveStopwatch()?.start(true)
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

