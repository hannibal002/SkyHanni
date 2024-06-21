package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGroup
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted

object Time : Element() {
    override fun getDisplay(): List<Any> {
        val symbol = getGroup(ScoreboardPattern.timePattern, ScoreboardData.sidebarLinesFormatted, "symbol") ?: ""
        val time = SkyBlockTime.now()
            .formatted(dayAndMonthElement = false, yearElement = false, timeFormat24h = displayConfig.skyblockTime24hFormat)
        return listOf(
            "§7$time $symbol",
        )
    }

    override fun showWhen() = true

    override val configLine = "§710:40pm §b☽"
}
