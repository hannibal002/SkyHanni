package at.hannibal2.skyhanni.utils.tracker

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

@Suppress("TooManyFunctions")
open class TimedTrackerData<Data : TrackerData<*>>(
    private val createNewSession: () -> Data,
) {
    fun reset() {
        sessions.clear()
    }

    fun reset(displayMode: DisplayMode) {
        sessions[displayMode]?.clear()
    }

    fun reset(displayMode: DisplayMode, string: String) {
        getData(displayMode, string)?.reset()
    }

    fun getEntries(displayMode: DisplayMode): MutableMap<String, Data>? {
        return sessions[displayMode]
    }

    fun getOrPutEntry(displayMode: DisplayMode): MutableMap.MutableEntry<String, Data> {
        return getOrPutEntry(displayMode, getDefaultName(displayMode))
    }

    fun getOrPutEntry(displayMode: DisplayMode, string: String): MutableMap.MutableEntry<String, Data> {
        val display = sessions.getOrPut(displayMode) { mutableMapOf() }
        display.getOrPut(string) { createNewSession() }
        return display.entries.first { it.key == string }
    }

    fun createEntry(displayMode: DisplayMode, string: String, data: Data) {
        val display = sessions.getOrPut(displayMode) { mutableMapOf() }
        display[string] = data
    }

    fun deleteEntry(displayMode: DisplayMode, string: String): Data? {
        val display = sessions[displayMode] ?: return null
        val data = display[string]
        if (getCurrentName(DisplayMode.SESSION) == string) {
            setCurrentName(DisplayMode.SESSION, getPrevNext(displayMode, string).second ?: "current")
        }
        display.remove(string)
        return data
    }

    fun getAllCurrentData(): Set<Data> {
        val set = mutableSetOf<Data>()
        sessions.keys.forEach {
            getCurrentData(it)?.let { it1 -> set.add(it1) }
        }
        return set
    }

    fun getOrPutData(displayMode: DisplayMode, string: String): Data {
        return getOrPutEntry(displayMode, string).value
    }

    fun getOrPutData(displayMode: DisplayMode): Data {
        return getOrPutEntry(displayMode).value
    }

    fun getData(displayMode: DisplayMode, string: String): Data? {
        return getEntries(displayMode)?.get(string)
    }

    fun getCurrentData(displayMode: DisplayMode): Data? {
        return getCurrentName(displayMode)?.let { getData(displayMode, it) }
    }

    fun getOrPutNewestData(displayMode: DisplayMode): Data {
        return getOrPutEntry(displayMode, getDefaultName(displayMode)).value
    }

    fun getOrPutCurrentData(displayMode: DisplayMode): Data {
        return getOrPutEntry(displayMode, getOrPutCurrentName(displayMode)).value
    }

    fun setCurrentName(displayMode: DisplayMode, string: String): String {
        val name = if (isCurrent(displayMode, string)) "current" else string
        currentDisplays[displayMode] = name
        return name
    }

    fun getOrPutCurrentName(displayMode: DisplayMode): String {
        var current = currentDisplays[displayMode]
        if (current == null) current = setCurrentName(displayMode, getDefaultName(displayMode))
        if (current == "current") {
            return getFromCurrent(displayMode)
        }
        return current
    }

    fun getCurrentName(displayMode: DisplayMode): String? {
        val current = currentDisplays[displayMode]
        if (current == "current") {
            return getFromCurrent(displayMode)
        }
        return current
    }

    fun isCurrent(displayMode: DisplayMode, string: String): Boolean {
        return string == getFromCurrent(displayMode)
    }

    fun isCurrent(displayMode: DisplayMode): Boolean {
        return (currentDisplays[displayMode] == "current" || currentDisplays.size == 1)
    }

    fun getFromCurrent(displayMode: DisplayMode): String {
        if (displayMode.isDate) return getCurrentDateName(displayMode)
        return when (displayMode) {
            DisplayMode.MAYOR -> return SkyBlockTime.now().year.toString()
            DisplayMode.SESSION -> return getMostRecentName(DisplayMode.SESSION) ?: "1"
            // these should never be labeled as current since they don't dynamically change
            else -> getMostRecentName(displayMode) ?: "current"
        }
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
        }.getOrElse {
            keys.firstOrNull()
        }
        return max?.let { runCatching { displayMode.fromValue(it) }.getOrNull() }
    }

    @Suppress("UNCHECKED_CAST")
    fun getPrevNext(displayMode: DisplayMode, current: String): Pair<String?, String?> {
        val keys = getEntries(displayMode)?.keys ?: return null to null

        val sortedKeys = runCatching {
            keys.map { key -> key to (displayMode.toValue(key) as Comparable<Any>) }
                .sortedBy { pair -> pair.second }
        }.getOrElse {
            keys.map { key -> key to key }
        }

        val index = sortedKeys.indexOfFirst { it.first == current }
        if (index == -1) return null to null

        val prev = sortedKeys.getOrNull(index - 1)?.first
        val next = sortedKeys.getOrNull(index + 1)?.first

        return prev to next
    }

    fun cleanEntries(config: TimedTrackerConfig) {
        sessions.keys.toList().forEach { displayMode ->
            cleanEntry(config, displayMode)
        }
    }

    fun cleanEntry(config: TimedTrackerConfig, displayMode: DisplayMode) {
        val keep = when (displayMode) {
            DisplayMode.DAY -> config.days
            DisplayMode.WEEK -> config.weeks
            DisplayMode.MONTH -> config.months
            DisplayMode.YEAR -> config.years
            DisplayMode.SESSION -> config.session
            else -> config.others
        }

        sessions[displayMode]?.let { map ->
            cleanEntries(map, keep, displayMode)
        }
    }


    fun cleanEntries(map: MutableMap<String, Data>, keepAmount: Int, displayMode: DisplayMode) {
        if (keepAmount <= 0) return

        val keysSorted = map.keys.sortedWith(
            compareBy {
                displayMode.toValue(it)
            }
        )

        val toRemove = keysSorted.dropLast(keepAmount)
        if (toRemove.isEmpty()) return

        toRemove.forEach { key ->
            map.remove(key)
        }
    }

    @Expose
    private val currentDisplays: MutableMap<DisplayMode, String> = mutableMapOf()

    @Expose
    private val sessions: MutableMap<DisplayMode, MutableMap<String, Data>> = EnumMap(DisplayMode::class.java)
}
