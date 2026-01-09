package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardNumberTrackingElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSowdust
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import kotlinx.coroutines.Job

// scoreboard
// scoreboard update event
object ScoreboardElementSowdust : ScoreboardElement(), CustomScoreboardNumberTrackingElement {
    override var previousAmount: Long = 0
    override var temporaryChangeDisplay: String? = null
    override val numberColor = "§2"
    override var currentJob: Job? = null

    override fun getDisplay(): String? {
        val sowdust = getSowdust()
        checkDifference(sowdust.formatLong())
        val line = formatStringNum(sowdust) + temporaryChangeDisplay.orEmpty()
        if (informationFilteringConfig.hideEmptyLines && line == "0") return null

        return CustomScoreboardUtils.formatNumberDisplay("Sowdust", line, numberColor)
    }

    override val configLine = "Sowdust: §23,210,307"

    override val elementPatterns = listOf(ScoreboardPattern.sowdustPattern, ScoreboardPattern.sowdustGainedPattern)

    override fun showIsland() = IslandTypeTags.GARDEN_ISLAND.inAny()
}
