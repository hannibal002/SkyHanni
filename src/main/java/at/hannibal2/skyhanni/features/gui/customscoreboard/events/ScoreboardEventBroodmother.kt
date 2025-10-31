package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.features.combat.SpidersDenApi

// scoreboard
// widget update event
object ScoreboardEventBroodmother : ScoreboardEvent() {
    override fun getDisplay() = TabWidget.BROODMOTHER.lines.map { it.trim() }

    override val configLine = "Broodmother§7: §eDormant"

    override val elementPatterns = listOf(SpidersDenApi.broodmotherPattern)

    override fun showIsland() = IslandType.SPIDER_DEN.isCurrent()
}
