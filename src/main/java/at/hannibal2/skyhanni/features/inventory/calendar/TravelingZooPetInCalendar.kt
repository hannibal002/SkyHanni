package at.hannibal2.skyhanni.features.inventory.calendar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.CalendarApi
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyblockSeason
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
    fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return

        if (CalendarApi.inCalendar) {
            val skyblockEvents = CalendarApi.parseCalendarItem(event.itemStack) ?: return
            for (sbEvent in skyblockEvents) {
                if (sbEvent.name == "Traveling Zoo") {
                    val pet = getZooPet(sbEvent.startTime) ?: return
                    event.toolTip.add(pet)
                }
            }
        }

        if (CalendarApi.inMainCalendar) {
            val sbEvent = CalendarApi.parseMainCalendarItem(event.itemStack) ?: return
            if (sbEvent.name == "Traveling Zoo") {
                val approximateTime = SkyBlockTime.fromTimeMark(sbEvent.startTime)
                val pet = getZooPet(approximateTime) ?: return
                event.toolTip.add(pet)
            }
        }
    }

    private fun getZooPet(time: SkyBlockTime): String? {
        val zooNumber = getTravelZooNumber(time) ?: return null
        val pet = ORINGO_PETS[zooNumber % ORINGO_PETS.size]
        return "§7Pet available: §6$pet"
    }

    private fun getTravelZooNumber(time: SkyBlockTime): Int? {
        val extraSeason = when (getSeasonByMonth(time.month).first) {
            SkyblockSeason.SUMMER -> 0
            SkyblockSeason.WINTER -> 1
            SkyblockSeason.AUTUMN, SkyblockSeason.SPRING -> return null
        }

        return (time.year * 2 + extraSeason)
    }

    fun isEnabled() = SkyHanniMod.feature.inventory.oringoPetInCalendar
}
