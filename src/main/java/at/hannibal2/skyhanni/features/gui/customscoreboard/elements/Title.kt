package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardLine.Companion.align
import at.hannibal2.skyhanni.utils.LorenzUtils

object Title : ScoreboardElement() {
    override fun getDisplay(): Any {
        val alignment = displayConfig.titleAndFooter.alignTitleAndFooter

        if (!LorenzUtils.inSkyBlock && !displayConfig.titleAndFooter.useCustomTitleOutsideSkyBlock) {
            return ScoreboardData.objectiveTitle align alignment
        }

        return if (displayConfig.titleAndFooter.useCustomTitle) {
            listOf(
                displayConfig.titleAndFooter.customTitle
                    .replace("&", "§")
                    .split("\\n")
                    .map { it align alignment },
            ).flatten()
        } else {
            ScoreboardData.objectiveTitle align alignment
        }
    }

    override val configLine = "§6§lSKYBLOCK"
}
