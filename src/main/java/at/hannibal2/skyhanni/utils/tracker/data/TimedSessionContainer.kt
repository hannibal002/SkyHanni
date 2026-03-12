package at.hannibal2.skyhanni.utils.tracker.data

import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.data.ElectionApi.getElectionYear
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.monthFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.yearFormatter
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DisplayMode
import com.google.gson.annotations.Expose
import java.time.LocalDate
import java.util.EnumMap

/**
 * Owns all session storage and navigation for a [TimedTrackerData] leaf.
 */
@Suppress("TooManyFunctions")
class TimedSessionContainer {

    @Expose
    val currentDisplays: MutableMap<DisplayMode, String?> = mutableMapOf()

    @Expose
    val sessions: MutableMap<DisplayMode, MutableMap<String, TimedTrackerData<*>>> = EnumMap(DisplayMode::class.java)

    fun reset(displayMode: DisplayMode? = null, string: String? = null) = when (displayMode) {
        null -> sessions.clear()
        else -> when (string) {
            null -> sessions[displayMode] = mutableMapOf()
            else -> getData(displayMode, string)?.reset()
        }
    }

    fun getEntries(displayMode: DisplayMode): MutableMap<String, TimedTrackerData<*>>? = sessions[displayMode]

    fun putEntry(displayMode: DisplayMode, string: String, data: TimedTrackerData<*>) {
        sessions.getOrPut(displayMode) { mutableMapOf() }[string] = data
    }

    fun deleteEntry(displayMode: DisplayMode, string: String): TimedTrackerData<*>? {
        val display = sessions[displayMode] ?: return null
        val data = display[string] ?: return null
        if (getCurrentName(DisplayMode.SESSION) == string) {
            // null = follow live; only pin if there is an adjacent entry to land on
            currentDisplays[DisplayMode.SESSION] = getPrevNext(displayMode, string).second
        }
        display.remove(string)
        return data
    }

    fun getAllCurrentData(): Set<TimedTrackerData<*>> = sessions.keys.mapNotNull { getCurrentData(it) }.toSet()

    fun getData(displayMode: DisplayMode, string: String): TimedTrackerData<*>? = getEntries(displayMode)?.get(string)

    fun getCurrentData(displayMode: DisplayMode): TimedTrackerData<*>? =
        getCurrentName(displayMode)?.let { getData(displayMode, it) }

    /**
     * Sets the current pointer for [displayMode]. Pass null to follow the live name.
     */
    fun setCurrentName(displayMode: DisplayMode, string: String?) {
        currentDisplays[displayMode] = string?.takeUnless { isCurrent(displayMode, it) }
    }

    /**
     * Returns the resolved current name for [displayMode], initializing the pointer to live if not
     * yet set. Always returns a non-null string.
     */
    fun resolveCurrentName(displayMode: DisplayMode): String {
        if (!currentDisplays.containsKey(displayMode)) currentDisplays[displayMode] = null
        return currentDisplays[displayMode] ?: getFromCurrent(displayMode)
    }

    /**
     * Returns the resolved current name only if a pointer has already been set for [displayMode],
     * otherwise null.
     */
    fun getCurrentName(displayMode: DisplayMode): String? {
        if (!currentDisplays.containsKey(displayMode)) return null
        return currentDisplays[displayMode] ?: getFromCurrent(displayMode)
    }

    fun isCurrent(displayMode: DisplayMode, string: String): Boolean = string == getFromCurrent(displayMode)

    fun isCurrent(displayMode: DisplayMode): Boolean =
        !currentDisplays.containsKey(displayMode) || currentDisplays[displayMode] == null

    fun getFromCurrent(displayMode: DisplayMode): String =
        if (displayMode.isDate) getCurrentDateName(displayMode)
        else when (displayMode) {
            DisplayMode.MAYOR -> SkyBlockTime.now().year.toString()
            DisplayMode.SESSION -> getMostRecentName(DisplayMode.SESSION) ?: "1"
            else -> getMostRecentName(displayMode) ?: "current"
        }

    fun getDefaultName(displayMode: DisplayMode): String {
        if (displayMode.isDate) return getCurrentDateName(displayMode)
        return when (displayMode) {
            DisplayMode.MAYOR -> SkyBlockTime.now().getElectionYear().toString()
            DisplayMode.SESSION -> getMostRecentName(displayMode) ?: "1"
            else -> displayMode.name.lowercase()
        }
    }

    fun getCurrentDateName(displayMode: DisplayMode): String {
        val now = LocalDate.now()
        return when (displayMode) {
            DisplayMode.DAY -> now.toString()
            DisplayMode.WEEK -> now.format(weekFormatter)
            DisplayMode.MONTH -> now.format(monthFormatter)
            DisplayMode.YEAR -> now.format(yearFormatter)
            else -> ""
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getMostRecentName(displayMode: DisplayMode): String? {
        val keys = getEntries(displayMode)?.keys ?: return null
        val max = runCatching {
            keys.maxOfOrNull { displayMode.toValue(it) as Comparable<Any> }
        }.getOrElse { keys.firstOrNull() }
        return max?.let { runCatching { displayMode.fromValue(it) }.getOrNull() }
    }

    @Suppress("UNCHECKED_CAST")
    fun getPrevNext(displayMode: DisplayMode, current: String): Pair<String?, String?> {
        val keys = getEntries(displayMode)?.keys ?: return null to null
        val sortedKeys = runCatching {
            keys.map { key -> key to (displayMode.toValue(key) as Comparable<Any>) }.sortedBy { it.second }
        }.getOrElse { keys.map { key -> key to key } }

        val index = sortedKeys.indexOfFirst { it.first == current }
        if (index == -1) return null to null
        return sortedKeys.getOrNull(index - 1)?.first to sortedKeys.getOrNull(index + 1)?.first
    }

    fun cleanEntries(config: TimedTrackerConfig) = sessions.keys.toList().forEach { cleanEntry(config, it) }

    fun cleanEntry(config: TimedTrackerConfig, displayMode: DisplayMode) {
        val keep = when (displayMode) {
            DisplayMode.DAY -> config.days
            DisplayMode.WEEK -> config.weeks
            DisplayMode.MONTH -> config.months
            DisplayMode.YEAR -> config.years
            DisplayMode.SESSION -> config.session
            else -> config.others
        }
        sessions[displayMode]?.let { dropOldest(it, keep, displayMode) }
    }

    private fun dropOldest(map: MutableMap<String, TimedTrackerData<*>>, keepAmount: Int, displayMode: DisplayMode) {
        if (keepAmount <= 0) return
        map.keys.sortedWith(compareBy { displayMode.toValue(it) }).dropLast(keepAmount).forEach { map.remove(it) }
    }
}
