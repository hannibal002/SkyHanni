package at.hannibal2.skyhanni.features.gui.bar.elements

import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted

object Date : BarElement() {
    override val configLine: String = "Date"
    override fun getString(): String = SkyBlockTime.now().formatted(hoursAndMinutesElement = false)
}
