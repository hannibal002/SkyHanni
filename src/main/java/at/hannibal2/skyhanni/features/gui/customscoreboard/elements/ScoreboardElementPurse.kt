package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.PurseApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardNumberTrackingElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import at.hannibal2.skyhanni.features.rift.RiftApi
import kotlinx.coroutines.Job

// internal
// purse change event (add total purse to event)
object ScoreboardElementPurse : ScoreboardElement(), CustomScoreboardNumberTrackingElement {
    override var previousAmount: Long = PurseApi.currentPurse.toLong()
    override var temporaryChangeDisplay: String? = null
    override val numberColor = "§6"
    override var currentJob: Job? = null

    override fun getDisplay(): String? {
        val currentPurse = PurseApi.currentPurse.toLong()
        checkDifference(currentPurse)
        val line = formatNumber(currentPurse) + temporaryChangeDisplay.orEmpty()
        if (informationFilteringConfig.hideEmptyLines && line == "0") return null

        return CustomScoreboardUtils.formatNumberDisplay("Purse", line, numberColor)
    }

    override val configLine = "Purse: §652,763,737"

    override val elementPatterns = listOf(PurseApi.coinsPattern)

    override fun showIsland() = !RiftApi.inRift()
}
