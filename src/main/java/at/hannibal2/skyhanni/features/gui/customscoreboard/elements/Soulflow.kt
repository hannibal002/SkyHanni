package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSoulflow
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Soulflow : Element() {
    override fun getDisplayPair(): List<Any> {
        val soulflow = getSoulflow()
        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && soulflow == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "§3$soulflow Soulflow"
                else -> "Soulflow: §3$soulflow"
            },
        )
    }

    override fun showWhen() = !inAnyIsland(IslandType.THE_RIFT)

    override val configLine = "Soulflow: §3761"
}
