package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches

object NewYear : Event() {
    override fun getDisplay() = listOf(getSbLines().first { ScoreboardPattern.newYearPattern.matches(it) })

    override fun showWhen() = ScoreboardPattern.newYearPattern.anyMatches(getSbLines())

    override val configLine = "§dNew Year Event!§f 24:25"
}
