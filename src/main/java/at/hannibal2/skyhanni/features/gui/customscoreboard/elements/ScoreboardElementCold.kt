package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.mining.ColdOverlay
import java.util.regex.Pattern

// internal
// update with cold update event
object ScoreboardElementCold : ScoreboardElement() {
    override fun getDisplay(): String? {
        val cold = -MiningApi.cold
        if (informationFilteringConfig.hideEmptyLines && cold == 0) return null

        return CustomScoreboardUtils.formatNumberDisplay("Cold", "$cold❄", "§b")
    }

    override val configLine = "Cold: §b0❄"

    override val elementPatterns: List<Pattern> = listOf(MiningApi.coldPattern)

    override fun showIsland(): Boolean = IslandTypeTag.IS_COLD.isInIsland()

    override fun showWhen(): Boolean = ColdOverlay.shouldShowCold()
}

// click: warp basecamp
