package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches

@SkyHanniModule
object SpidersDenAPI {
    fun isAtTopOfNest(): Boolean = ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.broodmotherPattern.matches(it) }
}
