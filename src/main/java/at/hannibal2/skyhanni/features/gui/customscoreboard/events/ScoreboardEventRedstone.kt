package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandTypeTags
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.firstMatches

// scoreboard
// scoreboard update event
object ScoreboardEventRedstone : ScoreboardEvent() {
    override fun getDisplay() = ScoreboardPattern.redstonePattern.firstMatches(getSBLines())

    override val configLine = "§e§l⚡ §cRedstone: §e§b7%"

    override val elementPatterns = listOf(ScoreboardPattern.redstonePattern)

    override fun showIsland() = IslandTypeTags.PRIVATE_ISLAND.inAny()
}
