package at.hannibal2.skyhanni.utils.tracker.data

import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DisplayMode
import com.google.gson.annotations.Expose

/**
 * Data leaf for timed trackers. Owns session creation via reflection, matching the pattern
 * used by [at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.createNewSession].
 * All storage and navigation is delegated to [sessionContainer].
 */
@Suppress("TooManyFunctions")
abstract class TimedTrackerData<T : SessionUptime> : TrackerData<T>() {

    @Expose
    val sessionContainer: TimedSessionContainer = TimedSessionContainer()

    override fun reset() {
        super.reset()
        sessionContainer.reset()
    }

    fun reset(displayMode: DisplayMode) = sessionContainer.reset(displayMode)
    fun reset(displayMode: DisplayMode, string: String) = sessionContainer.reset(displayMode, string)

    fun createNewSession(): TimedTrackerData<*> = this.javaClass.getConstructor().newInstance()

    fun getOrPutEntry(displayMode: DisplayMode): MutableMap.MutableEntry<String, TimedTrackerData<*>> =
        getOrPutEntry(displayMode, sessionContainer.getDefaultName(displayMode))

    fun getOrPutEntry(displayMode: DisplayMode, string: String): MutableMap.MutableEntry<String, TimedTrackerData<*>> {
        val display = sessionContainer.sessions.getOrPut(displayMode) { mutableMapOf() }
        display.getOrPut(string) { createNewSession() }
        return display.entries.first { it.key == string }
    }

    fun getEntries(displayMode: DisplayMode) = sessionContainer.getEntries(displayMode)

    fun createEntry(displayMode: DisplayMode, string: String, data: TimedTrackerData<*>) =
        sessionContainer.putEntry(displayMode, string, data)

    fun deleteEntry(displayMode: DisplayMode, string: String) = sessionContainer.deleteEntry(displayMode, string)

    fun getAllCurrentData() = sessionContainer.getAllCurrentData()

    fun getData(displayMode: DisplayMode, string: String) = sessionContainer.getData(displayMode, string)

    fun getCurrentData(displayMode: DisplayMode) = sessionContainer.getCurrentData(displayMode)

    fun getOrPutNewestData(displayMode: DisplayMode): TimedTrackerData<*> =
        getOrPutEntry(displayMode, sessionContainer.getDefaultName(displayMode)).value

    fun getOrPutCurrentData(displayMode: DisplayMode): TimedTrackerData<*> =
        getOrPutEntry(displayMode, sessionContainer.resolveCurrentName(displayMode)).value

    fun setCurrentName(displayMode: DisplayMode, string: String?) = sessionContainer.setCurrentName(displayMode, string)

    fun resolveCurrentName(displayMode: DisplayMode) = sessionContainer.resolveCurrentName(displayMode)

    fun getCurrentName(displayMode: DisplayMode) = sessionContainer.getCurrentName(displayMode)

    fun isCurrent(displayMode: DisplayMode, string: String) = sessionContainer.isCurrent(displayMode, string)

    fun isCurrent(displayMode: DisplayMode) = sessionContainer.isCurrent(displayMode)

    fun getMostRecentName(displayMode: DisplayMode) = sessionContainer.getMostRecentName(displayMode)

    fun getPrevNext(displayMode: DisplayMode, string: String) = sessionContainer.getPrevNext(displayMode, string)

    fun cleanEntries(config: TimedTrackerConfig) = sessionContainer.cleanEntries(config)

    fun cleanEntry(config: TimedTrackerConfig, displayMode: DisplayMode) =
        sessionContainer.cleanEntry(config, displayMode)
}
