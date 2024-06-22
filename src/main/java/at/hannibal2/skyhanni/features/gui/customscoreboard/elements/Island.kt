package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.utils.LorenzUtils

object Island : ScoreboardElement() {
    override fun getDisplay() = listOf("§7㋖ §a" + LorenzUtils.skyBlockIsland.displayName)

    override val configLine = "§7㋖ §aHub"
}
