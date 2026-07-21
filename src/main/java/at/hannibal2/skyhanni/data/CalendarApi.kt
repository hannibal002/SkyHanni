package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.time.Duration

@SkyHanniModule
object CalendarApi {
    private val group = RepoPattern.group("calendarapi")

    var inMainCalendar = false
        private set
    var inCalendar = false
        private set

    var calendarYear = 0
        private set
    var calendarMonth = 0
        private set

    /**
     * REGEX-TEST: Calendar and Events
     */
    private val calendarGuiPattern by group.pattern(
        "gui",
        "Calendar and Events",
    )

    /**
     * REGEX-TEST: Summer, Year 498
     * REGEX-TEST: Late Summer, Year 498
     * REGEX-TEST: Early Autumn, Year 498
     * REGEX-TEST: Autumn, Year 498
     * REGEX-TEST: Late Autumn, Year 498
     * REGEX-TEST: Early Winter, Year 498
     * REGEX-TEST: Winter, Year 498
     * REGEX-TEST: Late Winter, Year 498
     */
    private val calendarSeasonPattern by group.pattern(
        "date",
        "(?<season>(?:Early |Late )?(?:Spring|Summer|Autumn|Winter)), Year (?<year>\\d+)"
    )

    /**
     * REGEX-TEST: Day 3
     * REGEX-TEST: Day 2
     * REGEX-TEST: Day 1
     */
    val dayHeaderPattern by group.pattern(
        "day-header",
        "Day (?<dayNum>\\d+)"
    )

    /**
     * REGEX-TEST: All day: Traveling Zoo (40h)
     * REGEX-TEST: All day: Bonus Fishing Festival (40h)
     * REGEX-TEST: 12:00 am-11:59 pm: Jacob's Farming Contest (41h)
     * REGEX-TEST: All day: Bonus Fishing Festival (30h)
     * REGEX-TEST: 12:00 am-12:41 am: 61,711th Dark Auction (30h)
     * REGEX-TEST: All day: Bonus Fishing Festival
     * REGEX-TEST: 12:00 am-11:59 pm: Jacob's Farming Contest
     * REGEX-TEST: 12:00 am-12:41 am: 61,680th Dark Auction
     */
    val eventLinePattern by group.pattern(
        "event-line",
        """^(?<timePrefix>.*?):\s+(?<eventName>.*?)(?:\s+\((?<countdown>\d+h)\))?$"""
    )

    /**
     * REGEX-TEST: 12:00 am
     * REGEX-TEST: 11:59 pm
     * REGEX-TEST: 12:41 am
     */
    val skyblockTimePattern by group.pattern(
        "skyblock-time",
        "(?<hour>\\d+):(?<minute>\\d+)\\s*(?<period>am|pm)"
    )

    val mainCalendarStartsInPattern by group.pattern(
        "main.startsin",
        "Starts in: (?<time>(?:\\d\\d?[hms] ?)+)"
    )

    val mainCalendarDurationPattern by group.pattern(
        "main.duration",
        "Event lasts for: (?<time>(?:\\d\\d?[hms] ?)+)"
    )

    fun parseCalendarTooltip(tooltipLines: List<Component>): List<CalendarEvent>? {
        val line = tooltipLines.firstOrNull()?.string?.removeColor()?.trim() ?: return null
        val currentDay = dayHeaderPattern.matchMatcher(line) {
            group("dayNum")?.toInt()
        } ?: return null

        return tooltipLines.asSequence()
            .drop(1)
            .mapNotNull { component: Component ->
                val line = component.string.removeColor().trim()
                eventLinePattern.matchMatcher(line) {
                    val timePrefix = group("timePrefix") ?: return@matchMatcher null
                    val eventName = group("eventName")?.trim() ?: return@matchMatcher null
                    if (eventName.isBlank()) return@matchMatcher null
                    val (start, end) = parseEventTimes(currentDay, timePrefix)

                    CalendarEvent(
                        name = eventName,
                        startTime = start,
                        endTime = end
                    )
                }
            }.toList()
    }

    private fun parseEventTimes(day: Int, prefix: String): Pair<SkyBlockTime, SkyBlockTime> {
        if (prefix == "All day") {
            return SkyBlockTime(
                year = calendarYear,
                month = calendarMonth,
                day = day,
                hour = 0,
                minute = 0
            ) to SkyBlockTime(
                year = calendarYear,
                month = calendarMonth,
                day = day,
                hour = 23,
                minute = 59
            )
        }

        val split = prefix.split("-")

        if (split.size != 2) {
            return parseEventTimes(day, "All day")
        }

        val start = parseTime(split[0])
        val end = parseTime(split[1])

        return SkyBlockTime(
            year = calendarYear,
            month = calendarMonth,
            day = day,
            hour = start.first,
            minute = start.second
        ) to SkyBlockTime(
            year = calendarYear,
            month = calendarMonth,
            day = day,
            hour = end.first,
            minute = end.second
        )
    }

    private fun parseTime(input: String): Pair<Int, Int> {
        var result = 0 to 0

        skyblockTimePattern.matchMatcher(input.trim()) {
            var hour = group("hour").toInt()
            val minute = group("minute").toInt()
            val period = group("period")

            if (period == "pm" && hour != 12) {
                hour += 12
            }

            if (period == "am" && hour == 12) {
                hour = 0
            }

            result = hour to minute
        }

        return result
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGH)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (calendarGuiPattern.matches(event.inventoryName)) {
            inMainCalendar = true
        }
        calendarSeasonPattern.matchMatcher(event.inventoryName) {
            calendarMonth = SkyBlockTime.getSBMonthByName(group("season"))
            calendarYear = group("year").toInt()
            inCalendar = true
        }
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.LOW)
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!event.reopenSameName) {
            inMainCalendar = false
            inCalendar = false
        }
    }

    // Example:
    // Traveling Zoo
    // Starts in: 1h 58m 40s
    // Event lasts for: 1h
    //
    // Oringo The Taveling Zookeeper is
    // visiting SkyBlock with pets to trade!
    fun parseMainCalendarTooltip(rawTooltip: MutableList<Component>): MainCalendarEvent? {
        val tooltip = rawTooltip.map { it.string.removeColor().trim() }
        val eventName = tooltip.getOrNull(0) ?: return null

        val startTime = mainCalendarStartsInPattern.firstMatcher(tooltip) {
            val timeString = group("time")
            TimeUtils.getDurationOrNull(timeString)?.fromNow()
        }  ?: return null

        val duration = mainCalendarDurationPattern.firstMatcher(tooltip) {
            val timeString = group("time")
            TimeUtils.getDurationOrNull(timeString)
        } ?: return null

        return MainCalendarEvent(
            name = eventName,
            startTime = startTime,
            duration = duration
        )
    }
}

// Notice that this does not give an exact time and is an approximation of the event start time
data class MainCalendarEvent(
    val name: String,
    val startTime: SimpleTimeMark,
    val duration: Duration,
)

data class CalendarEvent(
    val name: String,
    val startTime: SkyBlockTime,
    val endTime: SkyBlockTime,
)
