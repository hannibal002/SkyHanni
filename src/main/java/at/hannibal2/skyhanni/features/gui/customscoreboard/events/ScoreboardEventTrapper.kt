package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.firstMatches
import at.hannibal2.hanni.utils.collection.CollectionUtils.addNotNull
import at.hannibal2.hanni.utils.collection.CollectionUtils.nextAfter

// scoreboard
// scoreboard update event
object ScoreboardEventTrapper : ScoreboardEvent() {
    override fun getDisplay() = buildList {
        addNotNull(ScoreboardPattern.peltsPattern.firstMatches(getSBLines()))
        ScoreboardPattern.mobLocationPattern.firstMatches(getSBLines())?.let {
            add(it)
            addNotNull(getSBLines().nextAfter(it))
        }
    }

    override val configLine = "Pelts: §5711\nTracker Mob Location:\n§bMushroom Gorge"

    override val elementPatterns = listOf(
        ScoreboardPattern.peltsPattern,
        ScoreboardPattern.mobLocationPattern,
    )

    override fun showIsland() = IslandType.THE_FARMING_ISLANDS.isCurrent()
}
