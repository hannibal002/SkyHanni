package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData

object ActiveTablist : ScoreboardEvent() {
    override fun getDisplay(): List<Any> {
        val currentActiveEvent = ScoreboardPattern.eventNamePattern.firstMatcher(TabListData.getTabList()) {
            group("name")
        } ?: return listOf()

        // Some Active Events are better not shown from the tablist,
        // but from other locations like the scoreboard
        val blockedEvents = listOf("Spooky Festival", "Carnival", "5th SkyBlock Anniversary", "New Year Celebration")
        if (blockedEvents.contains(currentActiveEvent.removeColor())) return listOf()
        val currentActiveEventTime = ScoreboardPattern.eventTimeEndsPattern.firstMatcher(TabListData.getTabList()) {
            group("time")
        } ?: "§cUnknown"

        return listOf(currentActiveEvent, " Ends in: §e$currentActiveEventTime")
    }

    override fun showWhen() = true

    override val configLine = "§7(All Active Tablist Events)\n§dHoppity's Hunt\n §fEnds in: §e26h"
}
