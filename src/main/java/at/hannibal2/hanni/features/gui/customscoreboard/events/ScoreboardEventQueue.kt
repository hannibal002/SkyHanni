package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventQueue : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines())

    override val configLine = "Queued: Glacite Mineshafts\nPosition: §b#45 §fSince: §a00:00"

    override val elementPatterns = listOf(
        ScoreboardPattern.queuePattern,
        ScoreboardPattern.queueTierPattern,
        ScoreboardPattern.queuePositionPattern,
        ScoreboardPattern.queueWaitingForLeaderPattern,
    )
}
