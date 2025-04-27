package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardNumberTrackingElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import kotlinx.coroutines.Job

// internal
// update with bits update event
object ScoreboardElementBits : ScoreboardElement(), CustomScoreboardNumberTrackingElement {
    override var previousAmount: Long = BitsApi.bits.toLong()
    override var temporaryChangeDisplay: String? = null
    override val numberColor = "§b"
    override var currentJob: Job? = null

    override fun getDisplay(): String? {
        val bits = BitsApi.bits.toLong()
        val bitsToClaim = BitsApi.bitsAvailable
        checkDifference(bits)
        val line = getBitsLine() + temporaryChangeDisplay.orEmpty()
        if (informationFilteringConfig.hideEmptyLines && BitsApi.bits == 0 && (bitsToClaim == -1 || bitsToClaim == 0)) return null

        return CustomScoreboardUtils.formatNumberDisplay("Bits", line, numberColor)
    }

    override fun showWhen() = !HypixelData.bingo

    override val configLine = "Bits: §b59,264"

    override val elementPatterns = listOf(BitsApi.bitsScoreboardPattern)

    override fun showIsland() = !inAnyIsland(IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)
}

// click: open /sbmenu
