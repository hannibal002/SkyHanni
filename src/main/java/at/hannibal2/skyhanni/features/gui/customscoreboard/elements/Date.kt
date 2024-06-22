package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted

object Date : ScoreboardElement() {
    override fun getDisplay() = listOf(SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false))

    override val configLine = "Late Summer 11th"
}
