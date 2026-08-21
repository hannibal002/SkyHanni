package at.hannibal2.skyhanni.features.inventory.calendar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.CalendarApi
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.inventory.calendar.TravelingZooPetInCalendar.ORINGO_PETS
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyblockSeason.Companion.getSeasonByMonth

@SkyHanniModule
object TravelingZooPetInCalendar {
    private val ORINGO_PETS = arrayOf(
        "Lion",
        "Monkey",
        "Elephant",
        "Giraffe",
        "Blue Whale",
        "Tiger"
    )

    @HandleEvent
    private fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return

        if (CalendarApi.inCalendar) {
            val skyblockEvents = CalendarApi.parseCalendarItem(event.itemStack) ?: return
            for (sbEvent in skyblockEvents) {
                if (sbEvent.name == "Traveling Zoo") {
                    event.toolTip.add("")
                    event.toolTip.add(getZooPet(sbEvent.startTime))
                }
            }
        }

        if (CalendarApi.inMainCalendar) {
            val sbEvent = CalendarApi.parseMainCalendarItem(event.itemStack) ?: return
            if (sbEvent.name == "Traveling Zoo") {
                val approximateTime = SkyBlockTime.fromTimeMark(sbEvent.startTime)
                event.toolTip.add("")
                event.toolTip.add(getZooPet(approximateTime))
            }
        }
    }

    private fun getZooPet(time: SkyBlockTime): String {
        val zooNumber = getTravelZooNumber(time)
        val pet = ORINGO_PETS[zooNumber % ORINGO_PETS.size]
        return "§7Pet available: §6$pet"
    }

    /**
     * Returns a continuous index over all Traveling Zoo events, two per SkyBlock year. Taken modulo the size of
     * [ORINGO_PETS], it selects the legendary pet Oringo offers at that event.
     *
     * The zoo starts on the first day of Early Summer and Early Winter, exactly on a month boundary. The start time
     * from the main calendar is only an approximation and can land slightly before or after that boundary, which
     * would be Late Spring or Late Autumn. Mapping whole half years instead of just those two seasons absorbs that
     * in both directions, with three SkyBlock months of tolerance either way.
     */
    private fun getTravelZooNumber(time: SkyBlockTime): Int {
        val zooOfYear = when (getSeasonByMonth(time.month).first) {
            SPRING, SUMMER -> 0
            AUTUMN, WINTER -> 1
        }

        return time.year * 2 + zooOfYear
    }

    fun isEnabled() = SkyHanniMod.feature.inventory.oringoPetInCalendar
}
