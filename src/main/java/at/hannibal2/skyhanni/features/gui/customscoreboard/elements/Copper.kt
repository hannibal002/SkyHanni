package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Copper : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        val copper = formatStringNum(getCopper())

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && copper == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "§c$copper Copper"
                else -> "Copper: §c$copper"
            },
        )
    }

    override fun showWhen() = inAnyIsland(IslandType.GARDEN)

    override val configLine = "Copper: §c23,495"
}
