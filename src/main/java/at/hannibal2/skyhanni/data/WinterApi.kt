package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.TimeUtils
import java.time.Month

object WinterApi {

    fun inWorkshop() = IslandType.WINTER.isCurrent()

    fun isDecember() = TimeUtils.getCurrentLocalDate().month == Month.DECEMBER
}
