package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches
import at.hannibal2.skyhanni.utils.SkyBlockUtils

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
