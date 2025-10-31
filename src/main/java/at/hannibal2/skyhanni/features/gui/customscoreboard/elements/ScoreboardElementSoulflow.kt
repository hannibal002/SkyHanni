package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardNumberTrackingElement
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSoulflow
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import kotlinx.coroutines.Job

// widget
// widget update event
object ScoreboardElementSoulflow : ScoreboardElement(), CustomScoreboardNumberTrackingElement {
    override var previousAmount: Long = 0
    override var temporaryChangeDisplay: String? = null
    override val numberColor = "§3"
    override var currentJob: Job? = null

    override fun getDisplay(): String? {
        val soulflow = getSoulflow()
        checkDifference(soulflow.formatLong())
        val line = formatStringNum(soulflow) + temporaryChangeDisplay.orEmpty()

        if (informationFilteringConfig.hideEmptyLines && line == "0") return null

        return CustomScoreboardUtils.formatNumberDisplay("Soulflow", line, numberColor)
    }

    override val configLine = "Soulflow: §3761"

    override fun showIsland() = !RiftApi.inRift()
}
