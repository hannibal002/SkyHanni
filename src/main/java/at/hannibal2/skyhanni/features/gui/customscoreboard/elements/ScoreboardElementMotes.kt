package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardNumberTrackingElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getMotes
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import kotlinx.coroutines.Job

// scoreboard
// scoreboard update event
object ScoreboardElementMotes : ScoreboardElement(), CustomScoreboardNumberTrackingElement {
    override var previousAmount: Long = 0
    override var temporaryChangeDisplay: String? = null
    override val numberColor = "§d"
    override var currentJob: Job? = null

    override fun getDisplay(): String? {
        val motes = getMotes()
        checkDifference(motes.formatLong())
        val line = formatStringNum(motes) + temporaryChangeDisplay.orEmpty()
        if (informationFilteringConfig.hideEmptyLines && line == "0") return null

        return CustomScoreboardUtils.formatNumberDisplay("Motes", line, numberColor)
    }

    override val configLine = "Motes: §d64,647"

    override val elementPatterns = listOf(ScoreboardPattern.motesPattern)

    override fun showIsland() = RiftApi.inRift()
}
