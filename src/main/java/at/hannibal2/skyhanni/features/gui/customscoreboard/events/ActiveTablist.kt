package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getTablistEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

// tablist (widget possibly)
object ActiveTablist : ScoreboardEvent() {
    // Some Active Events are better not shown from the tablist,
    // but from other locations like the scoreboard
    private val blockedEvents = listOf("Spooky Festival", "Carnival", "th SkyBlock Anniversary", "New Year Celebration")

    override fun getDisplay(): List<String>? {
        val name = getTablistEvent() ?: return null
        if (blockedEvents.contains(name.removeColor())) return null
        val currentActiveEventTime = ScoreboardPattern.eventTimeEndsPattern.firstMatcher(TabWidget.EVENT.lines) {
            group("time")
        } ?: return null

        return listOf(name, " Ends in: §e$currentActiveEventTime")
    }

    override fun showWhen() = TabWidget.EVENT.isActive

    override val configLine = "§7(All Active Tablist Events)\n§dHoppity's Hunt\n §fEnds in: §e26h"
}
