package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Bits : Element() {
    override fun getDisplay(): List<Any> {
        val bits = formatNumber(BitsAPI.bits.coerceAtLeast(0))
        val bitsToClaim = if (BitsAPI.bitsAvailable == -1) {
            "§cOpen Sbmenu§b"
        } else {
            formatNumber(BitsAPI.bitsAvailable.coerceAtLeast(0))
        }

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && bits == "0" && bitsToClaim == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "${getBitsLine()} Bits"
                else -> "Bits: ${getBitsLine()}"
            },
        )
    }

    override fun showWhen() = !HypixelData.bingo && !inAnyIsland(IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)

    override val configLine = "Bits: §b59,264"
}
