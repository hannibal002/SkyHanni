package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.currentIslandEvents
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.eventsConfig

// everything
object Events : ScoreboardElement() {
    override fun getDisplay() =
        if (eventsConfig.showAllActiveEvents) currentIslandEvents.mapNotNull { it.getLinesOrNull() }.flatten()
        else currentIslandEvents.firstNotNullOfOrNull { it.getLinesOrNull() }

    override val configLine = "§7Wide Range of Events\n§7(too much to show all)"
}
