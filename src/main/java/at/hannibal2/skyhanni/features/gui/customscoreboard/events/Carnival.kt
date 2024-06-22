package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches

object Carnival : ScoreboardEvent() {
    override fun getDisplay() = listOf(
        ScoreboardPattern.carnivalPattern,
        ScoreboardPattern.carnivalTokensPattern,
        ScoreboardPattern.carnivalTasksPattern,
        ScoreboardPattern.timeLeftPattern,
        ScoreboardPattern.carnivalCatchStreakPattern,
        ScoreboardPattern.carnivalFruitsPattern,
        ScoreboardPattern.carnivalAccuracyPattern,
        ScoreboardPattern.carnivalKillsPattern,
        ScoreboardPattern.carnivalScorePattern,
    ).allMatches(getSbLines())

    override fun showWhen() = ScoreboardPattern.carnivalPattern.anyMatches(getSbLines())

    override val configLine = "§7(All Carnival Lines)"

    override fun showIsland() = IslandType.HUB.isInIsland()
}
