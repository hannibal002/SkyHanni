package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches

object Anniversary : ScoreboardEvent() {
    override fun getDisplay() = listOf(getSbLines().first { ScoreboardPattern.anniversaryPattern.matches(it) })

    override fun showWhen() = ScoreboardPattern.anniversaryPattern.anyMatches(getSbLines())

    override val configLine = "§d5th Anniversary§f 167:59:54"
}
