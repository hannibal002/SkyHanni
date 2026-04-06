package at.hannibal2.skyhanni.utils.tracker.data

import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TimedTrackerConfig
import at.hannibal2.skyhanni.utils.tracker.DisplayMode
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import com.google.gson.annotations.Expose

/**
 * Data leaf for timed trackers. Owns session creation via reflection, matching the pattern
 * used by [at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.createNewSession].
 * All storage and navigation lives in [sessionContainer].
 */
@Suppress("AbstractClassCanBeConcreteClass")
abstract class TimedTrackerData<T : SessionUptime> : TrackerData<T>() {

    @Expose
    val sessionContainer: TimedSessionContainer = TimedSessionContainer()

    override fun reset() {
        super.reset()
        sessionContainer.reset()
    }

    fun reset(displayMode: DisplayMode, string: String? = null) =
        sessionContainer.reset(displayMode, string)

    fun createNewSession(): TimedTrackerData<*> = this.javaClass.getConstructor().newInstance()

    fun getOrPutEntry(displayMode: DisplayMode): MutableMap.MutableEntry<String, TimedTrackerData<*>> =
        getOrPutEntry(displayMode, sessionContainer.getDefaultName(displayMode))

    fun getOrPutEntry(displayMode: DisplayMode, string: String): MutableMap.MutableEntry<String, TimedTrackerData<*>> {
        val display = sessionContainer.sessions.getOrPut(displayMode) { mutableMapOf() }
        display.getOrPut(string) { createNewSession() }
        return display.entries.first { it.key == string }
    }

    fun getOrPutNewestData(displayMode: DisplayMode): TimedTrackerData<*> =
        getOrPutEntry(displayMode, sessionContainer.getDefaultName(displayMode)).value

    fun getOrPutCurrentData(displayMode: DisplayMode): TimedTrackerData<*> =
        getOrPutEntry(displayMode, sessionContainer.resolveCurrentName(displayMode)).value

    fun cleanEntries(config: TimedTrackerConfig) = sessionContainer.cleanEntries(config)

    fun cleanEntry(config: TimedTrackerConfig, displayMode: DisplayMode) =
        sessionContainer.cleanEntry(config, displayMode)
}
