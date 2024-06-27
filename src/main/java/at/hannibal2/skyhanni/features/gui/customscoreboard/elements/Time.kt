package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGroup
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.formatted

// internal and scoreboard
object Time : ScoreboardElement() {
    override fun getDisplay(): String {
        val symbol = getGroup(ScoreboardPattern.timePattern, ScoreboardData.sidebarLinesFormatted, "symbol") ?: ""
        val time = SkyBlockTime.now()
            .formatted(dayAndMonthElement = false, yearElement = false, timeFormat24h = displayConfig.skyblockTime24hFormat)
        return "§7$time $symbol".trim()
    }

    override val configLine = "§710:40pm §b☽"
}
