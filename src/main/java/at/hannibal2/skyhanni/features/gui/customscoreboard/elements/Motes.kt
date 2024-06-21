package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getMotes
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Motes : Element() {
    override fun getDisplay(): List<Any> {
        val motes = formatStringNum(getMotes())

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && motes == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "§d$motes Motes"
                else -> "Motes: §d$motes"
            },
        )
    }

    override fun showWhen() = inAnyIsland(IslandType.THE_RIFT)

    override val configLine = "Motes: §d64,647"
}
