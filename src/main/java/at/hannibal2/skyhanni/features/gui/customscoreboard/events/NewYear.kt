package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.matches

object NewYear : ScoreboardEvent() {
    override fun getDisplay() = listOf(getSbLines().first { ScoreboardPattern.newYearPattern.matches(it) })

    override val configLine = "§dNew Year Event!§f 24:25"
}
