package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getPurseEarned
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.features.rift.RiftAPI

object Purse : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        var purse = formatNumber(PurseAPI.currentPurse)

        if (!displayConfig.hideCoinsDifference) {
            purse += getPurseEarned() ?: ""
        }

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && purse == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "§6$purse Purse"
                else -> "Purse: §6$purse"
            },
        )
    }

    override val configLine = "Purse: §652,763,737"

    override fun showIsland() = !RiftAPI.inRift()
}
