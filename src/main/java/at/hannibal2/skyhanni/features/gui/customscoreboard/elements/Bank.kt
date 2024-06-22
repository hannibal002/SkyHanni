package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBank
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.features.rift.RiftAPI

object Bank : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        val bank = getBank()

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && (bank == "0" || bank == "0§7 / §60") -> HIDDEN
                displayConfig.displayNumbersFirst -> "§6$bank Bank"
                else -> "Bank: §6$bank"
            },
        )
    }

    override val configLine = "Bank: §6249M"

    override fun showIsland() = !RiftAPI.inRift()
}
