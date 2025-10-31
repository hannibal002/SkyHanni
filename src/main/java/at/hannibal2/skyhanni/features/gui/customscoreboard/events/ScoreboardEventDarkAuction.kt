package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches
import at.hannibal2.hanni.utils.RegexUtils.firstMatches
import at.hannibal2.hanni.utils.collection.CollectionUtils.addNotNull
import at.hannibal2.hanni.utils.collection.CollectionUtils.nextAfter

// scoreboard
// scoreboard update event or 1s
object ScoreboardEventDarkAuction : ScoreboardEvent() {
    override fun getDisplay() = buildList {
        addAll(listOf(ScoreboardPattern.startingInPattern, ScoreboardPattern.timeLeftPattern).allMatches(getSBLines()))

        ScoreboardPattern.darkAuctionCurrentItemPattern.firstMatches(getSBLines())?.let {
            add(it)
            addNotNull(getSBLines().nextAfter(it))
        }
    }

    override val configLine = "Time Left: §b11\nCurrent Item:\n §5Travel Scroll to Sirius"

    override val elementPatterns = listOf(
        ScoreboardPattern.startingInPattern,
        ScoreboardPattern.timeLeftPattern,
        ScoreboardPattern.darkAuctionCurrentItemPattern,
    )

    override fun showIsland() = IslandType.DARK_AUCTION.isCurrent()
}
