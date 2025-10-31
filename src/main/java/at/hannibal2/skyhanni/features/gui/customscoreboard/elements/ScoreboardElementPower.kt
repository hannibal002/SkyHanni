package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.data.MaxwellApi
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.utils.NumberUtil.addSeparators

// internal
// power update event?
object ScoreboardElementPower : ScoreboardElement() {
    override fun getDisplay(): String = MaxwellApi.currentPower?.let {
        CustomScoreboardUtils.formatNumberDisplay(
            "Power",
            it + if (displayConfig.maxwell.showMagicalPower) " §7(§6${MaxwellApi.magicalPower?.addSeparators()}§7)" else "",
            "§a",
        )
    } ?: "§cOpen \"Your Bags\"!"

    override val configLine = "Power: §aSighted §7(§61.263§7)"

    override fun showIsland() = !RiftApi.inRift()
}

// click: does a "your bags" command exist?
