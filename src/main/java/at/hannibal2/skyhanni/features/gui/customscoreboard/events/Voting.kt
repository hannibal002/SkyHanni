package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Voting : ScoreboardEvent() {
    override fun getDisplay() = buildList {
        val sbLines = getSbLines()

        val yearLine = ScoreboardPattern.yearVotesPattern.firstMatches(sbLines) ?: return listOf<String>()
        add(yearLine)

        if (sbLines.nextAfter(yearLine) == "§7Waiting for") {
            add("§7Waiting for")
            add("§7your vote...")
        } else {
            addAll(ScoreboardPattern.votesPattern.allMatches(sbLines))
        }
    }

    override val configLine = "§7(All Voting Lines)"

    override fun showIsland() = IslandType.HUB.isInIsland()
}
