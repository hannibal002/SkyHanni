package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches

object JacobMedals : Event() {
    override fun getDisplay() = ScoreboardPattern.medalsPattern.allMatches(getSbLines())

    override fun showWhen() = inAnyIsland(IslandType.GARDEN, IslandType.HUB)

    override val configLine = "§6§lGOLD §fmedals: §613\n§f§lSILVER §fmedals: §f3\n§c§lBRONZE §fmedals: §c4"
}
