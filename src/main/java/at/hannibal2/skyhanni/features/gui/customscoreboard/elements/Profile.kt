package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase

class Profile : Element() {
    override fun getDisplayPair() = listOf(CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase())

    override fun showWhen() = true

    override val configLine = "§7♲ Blueberry"
}
