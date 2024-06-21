package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig

class Title : Element() {
    override fun getDisplayPair() = if (displayConfig.titleAndFooter.useHypixelTitleAnimation) {
        listOf(ScoreboardData.objectiveTitle to displayConfig.titleAndFooter.alignTitleAndFooter)
    } else {
        listOf(
            displayConfig.titleAndFooter.customTitle
                .replace("&", "§")
                .split("\\n")
                .map { it to displayConfig.titleAndFooter.alignTitleAndFooter },
        ).flatten()
    }

    override fun showWhen() = true
}
