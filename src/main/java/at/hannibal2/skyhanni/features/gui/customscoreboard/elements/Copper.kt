package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN

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

    override val configLine = "Copper: §c23,495"

    override fun showIsland() = GardenAPI.inGarden()
}
