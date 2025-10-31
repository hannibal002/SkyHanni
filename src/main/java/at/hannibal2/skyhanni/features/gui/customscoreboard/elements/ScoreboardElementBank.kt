package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getBank
import at.hannibal2.hanni.features.rift.RiftApi

// widget
// update with widget update event
object ScoreboardElementBank : ScoreboardElement() {
    override fun getDisplay(): String? {
        val bank = getBank()
        if (informationFilteringConfig.hideEmptyLines && (bank == "0" || bank == "0§7 / §60")) return null

        return CustomScoreboardUtils.formatNumberDisplay("Bank", bank, "§6")
    }

    override val configLine = "Bank: §6249M"

    override fun showIsland() = !RiftApi.inRift()
}

// click: open /bank (does that even exist?)
