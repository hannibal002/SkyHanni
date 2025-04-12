package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardNumberTrackingElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import kotlinx.coroutines.Job

// scoreboard
// scoreboard update event
object ScoreboardElementCopper : ScoreboardElement(), CustomScoreboardNumberTrackingElement {
    override var previousAmount: Long = 0
    override var temporaryChangeDisplay: String? = null
    override val numberColor = "§c"
    override var currentJob: Job? = null

    override fun getDisplay(): String? {
        val copper = getCopper()
        checkDifference(copper.formatLong())
        val line = formatStringNum(copper) + temporaryChangeDisplay.orEmpty()
        if (informationFilteringConfig.hideEmptyLines && line == "0") return null

        return CustomScoreboardUtils.formatNumberDisplay("Copper", line, numberColor)
    }

    override val configLine = "Copper: §c23,495"

    override val elementPatterns = listOf(ScoreboardPattern.copperPattern)

    override fun showIsland() = IslandTypeTags.GARDEN_ISLAND.inAny()
}

// click: warp barn?
