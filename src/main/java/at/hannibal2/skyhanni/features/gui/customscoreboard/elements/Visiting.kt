package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Visiting : Element() {
    override fun getDisplayPair() = listOfNotNull(ScoreboardPattern.visitingPattern.firstMatches(ScoreboardData.sidebarLinesFormatted))

    override fun showWhen() = ScoreboardPattern.visitingPattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

    override val configLine = " §a✌ §7(§a1§7/6)"
}
