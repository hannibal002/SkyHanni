package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.WinterApi
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventWinter : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines()).filter { !it.endsWith("Soon!") }

    override val configLine = "§7(All Winter Event Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.winterEventStartPattern,
        ScoreboardPattern.winterNextWavePattern,
        ScoreboardPattern.winterWavePattern,
        ScoreboardPattern.winterMagmaLeftPattern,
        ScoreboardPattern.winterTotalDmgPattern,
        ScoreboardPattern.winterCubeDmgPattern,
    )

    override fun showIsland() = WinterApi.inWorkshop()
}
