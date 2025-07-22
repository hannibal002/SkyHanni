package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getElementsFromAny
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardConfigEventElement
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardLine
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty

// everything
// whenever an event gets updated
object ScoreboardElementEvents : ScoreboardElement() {
    override fun getDisplay(): List<ScoreboardLine>? = with(CustomScoreboard.currentIslandEvents) {
        if (CustomScoreboard.displayConfig.events.showAllActiveEvents) {
            mapNotNull { it.getLines().takeIfNotEmpty() }.flatten()
        } else {
            firstNotNullOfOrNull { it.getLines().takeIfNotEmpty() }
        }
    }

    override val configLine = "§7Wide Range of Events\n§7(too much to show all)"

    override val elementPatterns get() =
        ScoreboardConfigEventElement.entries.filter { it.event.showIsland() }.flatMap { it.event.elementPatterns }

    override fun getLines(): List<ScoreboardLine> = if (showWhen()) getElementsFromAny(getDisplay()) else listOf()
}
