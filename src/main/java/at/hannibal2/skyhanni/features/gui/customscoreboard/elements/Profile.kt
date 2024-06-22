package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase

object Profile : ScoreboardElement() {
    override fun getDisplay() = CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase()

    override val configLine = "§7♲ Blueberry"
}
