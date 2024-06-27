package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

// internal and scoreboard
object Location : ScoreboardElement() {
    override fun getDisplay() = listOfNotNull(
        HypixelData.skyBlockAreaWithSymbol,
        ScoreboardPattern.plotPattern.firstMatches(ScoreboardData.sidebarLinesFormatted),
    )

    override val configLine = "§7⏣ §bVillage"
}
