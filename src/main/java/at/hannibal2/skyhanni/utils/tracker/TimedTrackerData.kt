package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.TimedTrackerConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.TimeUtils.dayToLocalDate
import at.hannibal2.skyhanni.utils.TimeUtils.monthFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.weekFormatter
import at.hannibal2.skyhanni.utils.TimeUtils.yearFormatter
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.DisplayMode
import com.google.gson.annotations.Expose
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

abstract class TimedTrackerData<Data : TrackerData<T>, T : SessionUptime>(
    session: KClass<T>,
    private val createNewSession: () -> Data,
) : TrackerData<T>(session) {
    @SkyHanniModule
    companion object TrackerManager {
        val trackerSet = mutableSetOf<TimedTrackerData<*, *>>()

        @HandleEvent
        fun onConfigLoad(event: ConfigLoadEvent) {
            trackerSet.forEach { it.cleanEntries() }
        }
    }

    val tempConfig get() = SkyHanniMod.feature.misc.timedTracker
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
        sessions.keys.forEach { cleanEntry(it, config) }
    }

    fun cleanEntry(displayMode: DisplayMode, config: TimedTrackerConfig) {
        when (displayMode) {
            DisplayMode.DAY -> sessions[displayMode]?.cleanEntries(config.days)
            DisplayMode.WEEK -> sessions[displayMode]?.cleanEntries(config.weeks)
            DisplayMode.MONTH -> sessions[displayMode]?.cleanEntries(config.months)
            DisplayMode.YEAR -> sessions[displayMode]?.cleanEntries(config.years)
            else -> return
        }
    }

    private fun MutableMap<String, Data>.cleanEntries(keepAmount: Int) {
        val sorted = this.entries.sortedBy { it.key.dayToLocalDate() }
        val toKeep = sorted.takeLast(keepAmount)
        this.clear()
        this.putAll(toKeep.associate { it.toPair() })
    }

    @Expose
    private var sessions: MutableMap<DisplayMode, MutableMap<String, Data>> = EnumMap(DisplayMode::class.java)

}
