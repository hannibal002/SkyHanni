package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches
import at.hannibal2.hanni.utils.SkyBlockUtils

// scoreboard
// scoreboard update event
object ScoreboardEventDojo : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines())

    override fun showWhen() = SkyBlockUtils.graphArea in listOf("Dojo", "Dojo Arena")

    override val configLine = "§7(All Dojo Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.dojoChallengePattern,
        ScoreboardPattern.dojoDifficultyPattern,
        ScoreboardPattern.dojoPointsPattern,
        ScoreboardPattern.dojoTimePattern,
    )

    override fun showIsland() = IslandType.CRIMSON_ISLE.isCurrent()
}
