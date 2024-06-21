package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Broodmother : Event() {
    override fun getDisplay() = listOfNotNull(ScoreboardPattern.broodmotherPattern.firstMatches(getSbLines()))

    override fun showWhen() = IslandType.SPIDER_DEN.isInIsland()

    override val configLine = "§4Broodmother§7: §eDormant"
}
