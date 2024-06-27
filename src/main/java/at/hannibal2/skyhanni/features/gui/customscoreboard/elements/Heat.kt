package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getHeat
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches

// scoreboard
object Heat : ScoreboardElement() {
    override fun getDisplay(): String? {
        val heat = getHeat()

        return when {
            informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> null
            displayConfig.displayNumbersFirst -> (heat ?: "§c♨ 0") + " Heat"
            else -> "Heat: " + (heat ?: "§c♨ 0")
        }
    }

    override fun showWhen() = ScoreboardPattern.heatPattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

    override val configLine = "Heat: §c♨ 14"

    override fun showIsland() = MiningAPI.inCrystalHollows()
}
