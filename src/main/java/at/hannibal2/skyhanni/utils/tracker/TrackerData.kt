package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.Stopwatch
import com.google.gson.annotations.Expose
import kotlin.reflect.KClass
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackerData<T : SessionUptime>(
    private val uptimeClass: KClass<T>,
) : Resettable {
    @Expose
    private var migrated = false

    @Expose
    @SerializedName("sessionUptime")
    private val sessionUptimeInternal: MutableMap<SessionUptime?, Stopwatch?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    private val sessionUptime: MutableMap<SessionUptime, Stopwatch>
        get() = sessionUptimeInternal as MutableMap<SessionUptime, Stopwatch>


    override fun reset() {
        super.reset()
        sessionUptime.values.forEach { it.reset() }
    }

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
            if (swapExtraTime) getActiveStopwatch()?.add(duration ?: 0.seconds)
        }
        getActiveStopwatch()?.start(true)
    }

    fun getTotalUptime(): Duration {
        if (!migrated) migrateData()
        val entries = if (uptimeClass == SessionUptime.Garden::class) {
            sessionUptime.entries.filter { SkyHanniMod.feature.garden.trackerUptimeSettings.types.get().contains(it.key.garden) }
        } else sessionUptime.entries

        var uptime = Duration.ZERO
        entries.forEach { entry ->
            uptime += entry.value.getDuration()
        }
        return uptime
    }

    private fun migrateData() {
        migrated = true
        // Old config versions may still hold null keys or values
        sessionUptimeInternal.entries.removeAll { it.key == null || it.value == null }

        when (uptimeClass) {
            SessionUptime.Normal::class -> {
                filterAndRemove(uptimeClass, SessionUptime.Normal(NormalSession.NORMAL))
            }

            SessionUptime.Garden::class -> {
                filterAndRemove(uptimeClass, SessionUptime.Garden(GardenSession.UNKNOWN))
            }
        }
    }

    private fun filterAndRemove(entryType: KClass<out SessionUptime>, migratedSessionType: SessionUptime) {
        val entries = sessionUptime.entries.filter { entry ->
            !entryType.isInstance(entry.key)
        }
        if (entries.isEmpty()) return
        entries.forEach { entry ->
            val unknown = sessionUptime.getOrPut(migratedSessionType) { Stopwatch() }
            unknown.add(entry.value.getDuration())
            sessionUptime.remove(entry.key)
        }
    }
}

