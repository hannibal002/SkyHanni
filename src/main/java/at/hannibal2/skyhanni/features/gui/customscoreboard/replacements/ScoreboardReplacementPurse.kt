package at.hannibal2.skyhanni.features.gui.customscoreboard.replacements

import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber

object ScoreboardReplacementPurse : ScoreboardReplacements() {
    override val trigger = "%purse%"
    override val name = "Purse"
    override fun replacement(): String = formatNumber(PurseAPI.getPurse())
}
