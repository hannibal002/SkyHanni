package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.TimeUtils
import java.time.Month

object WinterAPI {

    fun isDecember() = TimeUtils.getCurrentLocalDate().month == Month.DECEMBER
}
