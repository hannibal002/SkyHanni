package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object ScoreboardEventSafari : ScoreboardEvent() {
    override fun getDisplay() = buildList {
        ScoreboardPattern.capturedMobsPattern.firstMatches(getSBLines())?.let { add(it) }
    }

    override val configLine = "§7(All Safari Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.capturedMobsPattern,
    )

}
