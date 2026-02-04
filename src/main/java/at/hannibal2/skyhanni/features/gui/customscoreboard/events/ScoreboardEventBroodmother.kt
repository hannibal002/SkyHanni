package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.features.combat.SpidersDenApi
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat

// scoreboard
// widget update event
object ScoreboardEventBroodmother : ScoreboardEvent() {
    override fun getDisplay() = TabWidget.BROODMOTHER.lines.map { it.formattedTextCompat().trim() }

    override val configLine = "Broodmother§7: §eDormant"

    override val elementPatterns = listOf(SpidersDenApi.broodmotherPattern)

    override fun showIsland() = IslandType.SPIDER_DEN.isCurrent()
}
