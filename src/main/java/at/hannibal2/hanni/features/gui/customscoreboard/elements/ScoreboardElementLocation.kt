package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.data.ScoreboardData
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.firstMatches

// internal and scoreboard
// area change event and on scoreboard update in garden
object ScoreboardElementLocation : ScoreboardElement() {
    override fun getDisplay() = listOf(
        HypixelData.skyBlockAreaWithSymbol,
        ScoreboardPattern.plotPattern.firstMatches(ScoreboardData.sidebarLinesFormatted),
    )

    override val configLine = "§7⏣ §bVillage"

    override val elementPatterns = listOf(ScoreboardPattern.locationPattern, ScoreboardPattern.plotPattern)
}
