package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.TabListData

object StartingSoonTablist : Event() {
    override fun getDisplay(): List<Any> {
        val soonActiveEvent = ScoreboardPattern.eventNamePattern.firstMatcher(TabListData.getTabList()) {
            group("name")
        } ?: return emptyList()

        val soonActiveEventTime = TabListData.getTabList().firstOrNull { ScoreboardPattern.eventTimeStartsPattern.matches(it) }
            ?.let {
                ScoreboardPattern.eventTimeStartsPattern.matchMatcher(it) {
                    group("time")
                }
            }

        return listOf(soonActiveEvent, " Starts in: §e$soonActiveEventTime")
    }

    override fun showWhen() = true

    override val configLine = "§7(All Starting Soon Tablist Events)\n§6Mining Fiesta\n §fStarts in: §e52min"
}
