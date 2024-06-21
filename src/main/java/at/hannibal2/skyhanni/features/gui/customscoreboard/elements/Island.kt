package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.utils.LorenzUtils

object Island : Element() {
    override fun getDisplayPair() = listOf("§7㋖ §a" + LorenzUtils.skyBlockIsland.displayName)

    override fun showWhen() = true

    override val configLine = "§7㋖ §aHub"
}
