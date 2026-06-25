package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

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
     * REGEX-TEST: 1d 4h 20m
     */
    val timeComponentPattern by group.pattern(
        "time-component",
        "(?<amount>\\d+)(?<unit>[dhms])"
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

    fun parseTooltip(tooltipLines: List<Component>): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        var currentDay = 1
        for (component in tooltipLines) {
            val line = component.string.removeColor().trim()
            dayHeaderPattern.matchMatcher(line) {
                currentDay = group("dayNum").toInt()
                return@matchMatcher
            }

            eventLinePattern.matchMatcher(line) {
                val timePrefix = group("timePrefix").ifBlank { "All day" }
                val eventName = group("eventName").trim()
                if (eventName.isBlank()) return@matchMatcher
                val (start, end) = parseEventTimes(currentDay, timePrefix)
                events.add(
                    CalendarEvent(
                        name = eventName,
                        startTime = start,
                        endTime = end
                    )
                )
            }
        }

        return events
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
}

data class CalendarEvent(
    val name: String,
    val startTime: SkyBlockTime,
    val endTime: SkyBlockTime,
)
