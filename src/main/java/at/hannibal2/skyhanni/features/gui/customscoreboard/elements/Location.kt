package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Location : Element() {
    override fun getDisplay() = buildList {
        addNotNull(HypixelData.skyBlockAreaWithSymbol)
        addNotNull(ScoreboardPattern.plotPattern.firstMatches(ScoreboardData.sidebarLinesFormatted))
    }

    override fun showWhen() = true

    override val configLine = "§7⏣ §bVillage"
}
