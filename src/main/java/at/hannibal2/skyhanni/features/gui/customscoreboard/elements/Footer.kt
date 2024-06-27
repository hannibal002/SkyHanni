package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardLine.Companion.align

// internal
object Footer : ScoreboardElement() {
    override fun getDisplay() = listOf(
        displayConfig.titleAndFooter.customFooter
            .replace("&", "§")
            .split("\\n")
            .map { it align displayConfig.titleAndFooter.alignTitleAndFooter },
    ).flatten()

    override val configLine = "§ewww.hypixel.net"
}
