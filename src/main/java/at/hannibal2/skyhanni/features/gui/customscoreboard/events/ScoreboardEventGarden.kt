package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches

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

    override fun showIsland() = IslandTypeTag.GARDEN_ISLAND.isInIsland()
}
