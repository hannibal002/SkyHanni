package at.hannibal2.skyhanni.features.gui.customscoreboard.replacements

import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted

object Year : ScoreboardReplacements() {
    override val trigger = "%year%"
    override val name = "Year"
    override fun replacement(): String = SkyBlockTime.now().formatted(dayAndMonthElement = false, hoursAndMinutesElement = false)
}
