package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.misc.ServerRestartTitle
import at.hannibal2.hanni.utils.RegexUtils.firstMatches

// scoreboard
// scoreboard update event
object ScoreboardEventServerClose : ScoreboardEvent() {
    override fun getDisplay() =
        ServerRestartTitle.restartingGreedyPattern.firstMatches(getSBLines())?.split("§8")?.getOrNull(0)

    override val configLine = "§cServer closing soon!"

    override val elementPatterns = listOf(ServerRestartTitle.restartingGreedyPattern)
}
