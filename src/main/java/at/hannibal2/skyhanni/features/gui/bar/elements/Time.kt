package at.hannibal2.skyhanni.features.gui.bar.elements

import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted

object Time : BarElement() {
    override val configLine: String = "Time"
    override fun getString(): String = SkyBlockTime.now().formatted(yearElement = false, dayAndMonthElement = false)
}
