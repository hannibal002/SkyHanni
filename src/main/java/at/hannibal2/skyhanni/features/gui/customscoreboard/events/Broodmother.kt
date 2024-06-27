package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

// scoreboard
// scoreboard update event
object Broodmother : ScoreboardEvent() {
    override fun getDisplay() = ScoreboardPattern.broodmotherPattern.firstMatches(getSbLines())

    override val configLine = "§4Broodmother§7: §eDormant"

    override fun showIsland() = IslandType.SPIDER_DEN.isInIsland()
}
