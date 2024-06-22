package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig

object Footer : ScoreboardElement() {
    override fun getDisplay() = listOf(
        displayConfig.titleAndFooter.customFooter
            .replace("&", "§")
            .split("\\n")
            .map { it to displayConfig.titleAndFooter.alignTitleAndFooter },
    ).flatten()

    override fun showWhen() = true

    override val configLine = "§ewww.hypixel.net"
}
