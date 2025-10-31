package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.firstMatches

// scoreboard
// scoreboard update event
object ScoreboardEventAnniversary : ScoreboardEvent() {
    override fun getDisplay() = ScoreboardPattern.anniversaryPattern.firstMatches(getSBLines())

    override val configLine = "§d5th Anniversary§f 167:59:54"

    override val elementPatterns = listOf(ScoreboardPattern.anniversaryPattern)
}
