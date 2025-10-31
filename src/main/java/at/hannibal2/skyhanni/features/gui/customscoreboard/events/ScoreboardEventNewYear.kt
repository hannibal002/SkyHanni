package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.firstMatches

// scoreboard
// scoreboard update event
object ScoreboardEventNewYear : ScoreboardEvent() {
    override fun getDisplay() = ScoreboardPattern.newYearPattern.firstMatches(getSBLines())

    override val configLine = "§dNew Year Event!§f 24:25"

    override val elementPatterns = listOf(ScoreboardPattern.newYearPattern)
}
