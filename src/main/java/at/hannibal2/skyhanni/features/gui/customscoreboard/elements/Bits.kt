package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

// internal
// update with bits update event
object Bits : ScoreboardElement() {
    override fun getDisplay(): String? {
        val bits = formatNumber(BitsAPI.bits.coerceAtLeast(0))
        val bitsToClaim = if (BitsAPI.bitsAvailable == -1) "§cOpen Sbmenu§b"
        else formatNumber(BitsAPI.bitsAvailable.coerceAtLeast(0))

        return when {
            informationFilteringConfig.hideEmptyLines && bits == "0" && bitsToClaim == "0" -> null
            displayConfig.displayNumbersFirst -> "${getBitsLine()} Bits"
            else -> "Bits: ${getBitsLine()}"
        }
    }

    override fun showWhen() = !HypixelData.bingo

    override val configLine = "Bits: §b59,264"

    override fun showIsland() = !inAnyIsland(IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)
}
