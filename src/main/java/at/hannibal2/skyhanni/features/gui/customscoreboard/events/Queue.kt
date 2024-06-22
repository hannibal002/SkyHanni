package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches

object Queue : ScoreboardEvent() {
    override fun getDisplay() = listOf(
        ScoreboardPattern.queuePattern,
        ScoreboardPattern.queuePositionPattern,
    ).allMatches(getSbLines())

    override fun showWhen() = true

    override val configLine = "Queued: Glacite Mineshafts\nPosition: §b#45 §fSince: §a00:00"
}
