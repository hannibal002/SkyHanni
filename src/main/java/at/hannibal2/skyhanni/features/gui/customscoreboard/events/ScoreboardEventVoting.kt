package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventVoting : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines())

    // TODO: add area check

    override val configLine = "§7(All Voting Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.yearVotesPattern,
        ScoreboardPattern.votesPattern,
        ScoreboardPattern.waitingForVotePattern,
    )

    override fun showIsland() = IslandType.HUB.isCurrent()
}
