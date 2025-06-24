package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sublistAfter

// scoreboard
// scoreboard update event
object ScoreboardEventGalatea : ScoreboardEvent() {

    override fun getDisplay() = buildList {
        ScoreboardPattern.whispersPattern.firstMatches(getSBLines())?.let { add(it) }
        ScoreboardPattern.hotfPattern.firstMatches(getSBLines())?.let { add(it) }
        ScoreboardPattern.agathasContestPattern.firstMatches(getSBLines())?.let { line ->
            add(line)
            addAll(
                getSBLines().sublistAfter(line, amount = 2)
                    .filter { !ScoreboardPattern.footerPattern.matches(it) },
            )
        }
    }

    override val configLine = "§7(All Galatea Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.whispersPattern,
        ScoreboardPattern.hotfPattern,
        ScoreboardPattern.agathasContestPattern,
    )

    override fun showIsland() = IslandType.GALATEA.isValidIsland()
}
