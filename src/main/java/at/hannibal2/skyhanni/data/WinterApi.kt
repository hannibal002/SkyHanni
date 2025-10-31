package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.TimeUtils
import java.time.Month

@HanniModule
object WinterApi {

    private var inArea = false

    fun inWorkshop() = IslandType.WINTER.isCurrent()

    fun inGlacialCave() = inWorkshop() && inArea

    fun isDecember() = TimeUtils.getCurrentLocalDate().month == Month.DECEMBER

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inArea = event.area == "Glacial Cave"
    }
}
