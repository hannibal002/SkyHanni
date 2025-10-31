package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.data.IslandTypeTags
import at.hannibal2.hanni.data.MiningApi
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils

// internal
// update with cold update event
object ScoreboardElementCold : ScoreboardElement() {
    override fun getDisplay(): String? {
        val cold = -MiningApi.cold
        if (informationFilteringConfig.hideEmptyLines && cold == 0) return null

        return CustomScoreboardUtils.formatNumberDisplay("Cold", "$cold❄", "§b")
    }

    override val configLine = "Cold: §b0❄"

    override val elementPatterns = listOf(MiningApi.coldPattern)

    override fun showIsland() = IslandTypeTags.IS_COLD.inAny()
}

// click: warp basecamp
