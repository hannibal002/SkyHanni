package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandTypeTags
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventGarden : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines()).map { it.trim() }

    override val configLine = "§7(All Garden Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.lockedPattern,
        ScoreboardPattern.pastingPattern,
        ScoreboardPattern.cleanUpPattern,
    )

    override fun showIsland() = IslandTypeTags.GARDEN_ISLAND.inAny()
}
