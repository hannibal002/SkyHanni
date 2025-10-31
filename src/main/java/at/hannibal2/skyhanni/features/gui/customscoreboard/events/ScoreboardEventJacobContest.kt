package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.firstMatches
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.collection.CollectionUtils.sublistAfter

// scoreboard
// scoreboard update event
object ScoreboardEventJacobContest : ScoreboardEvent() {
    // TODO: Use patterns instead of sublistAfter
    override fun getDisplay() = buildList {
        ScoreboardPattern.jacobsContestPattern.firstMatches(getSBLines())?.let { line ->
            add(line)
            addAll(
                getSBLines().sublistAfter(line, amount = 3)
                    .filter { !ScoreboardPattern.footerPattern.matches(it) },
            )
        }
    }

    override val configLine: String = "§eJacob's Contest\n§e○ §fCarrot §a18m17s\n Collected §e8,264"

    override val elementPatterns = listOf(ScoreboardPattern.jacobsContestPattern)
}

