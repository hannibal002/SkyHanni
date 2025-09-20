package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.TimeUtils.dayToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.monthFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.monthToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.weekFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.yearFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.yearToLocalDate
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DisplayMode
import com.google.gson.annotations.Expose
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

class TimedTrackerData<Data : TrackerData<T>, T : SessionUptime>(
    session: KClass<T>,
    private val createNewSession: () -> Data,
) : TrackerData<T>(session) {
    val tempConfig get() = SkyHanniMod.feature.misc.tracker.timedTracker
    override fun resetData() {
        sessions = EnumMap(DisplayMode::class.java)
    }

    fun getOrPutEntry(displayMode: DisplayMode, date: LocalDate = LocalDate.now()): Data {
        val key = when (displayMode) {
            DisplayMode.TOTAL, DisplayMode.SESSION -> displayMode.name.lowercase()
            DisplayMode.MAYOR -> return createNewSession()
            DisplayMode.DAY -> date.toString()
            DisplayMode.WEEK -> date.format(weekFormatter)
            DisplayMode.MONTH -> date.format(monthFormatter)
            DisplayMode.YEAR -> date.format(yearFormatter)
        }
        val display = sessions.getOrPut(displayMode) { mutableMapOf() }
        return display.getOrPut(key) { createNewSession() }
    }

    fun getEntries(displayMode: DisplayMode): MutableMap<String, Data>? {
        return sessions[displayMode]
    }

    fun getEntry(displayMode: DisplayMode, date: LocalDate = LocalDate.now()): Data? {
        val key = when (displayMode) {
            DisplayMode.TOTAL, DisplayMode.SESSION -> displayMode.name.lowercase()
            DisplayMode.MAYOR -> return null
            DisplayMode.DAY -> date.toString()
            DisplayMode.WEEK -> date.format(weekFormatter)
            DisplayMode.MONTH -> date.format(monthFormatter)
            DisplayMode.YEAR -> date.format(yearFormatter)
        }
        return getEntries(displayMode)?.get(key)
    }

    fun cleanEntries(config: TimedTrackerConfig = tempConfig) {
        ChatUtils.debug("Cleaning Tracker Entries (before): ${sessions.mapValues { it.value.size }}")
        ChatUtils.debug(sessions.toString())
        sessions.keys.toList().forEach { displayMode ->
            val keep = when (displayMode) {
                DisplayMode.DAY   -> config.days
                DisplayMode.WEEK  -> config.weeks
                DisplayMode.MONTH -> config.months
                DisplayMode.YEAR  -> config.years
                else -> null
            } ?: return@forEach

            sessions[displayMode]?.let { map ->
                cleanEntries(map, keep, displayMode)
            }
        }
        ChatUtils.debug("Cleaning Tracker Entries (after): ${sessions.mapValues { it.value.size }}")
    }


    private fun cleanEntries(map: MutableMap<String, Data>, keepAmount: Int, displayMode: DisplayMode) {
        if (keepAmount <= 0) return

        val keysSorted = map.keys.sortedBy {
            when (displayMode) {
                DisplayMode.DAY -> it.dayToLocalDate()
                DisplayMode.WEEK -> it.weekToLocalDate()
                DisplayMode.MONTH -> it.monthToLocalDate()
                DisplayMode.YEAR -> it.yearToLocalDate()
                else -> null
            }
        }

        val toRemove = keysSorted.dropLast(keepAmount)

        if (toRemove.isEmpty()) return

        ChatUtils.debug("Removing ${toRemove.size} entries: $toRemove")
        toRemove.forEach { key ->
            map.remove(key)
        }
    }

    @Expose
    private var sessions: MutableMap<DisplayMode, MutableMap<String, Data>> = EnumMap(DisplayMode::class.java)

}
