package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.TimeUtils
import java.time.Month

@SkyHanniModule
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
