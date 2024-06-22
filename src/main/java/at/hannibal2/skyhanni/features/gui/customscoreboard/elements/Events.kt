package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEventManager

object Events : ScoreboardElement() {
    override fun getDisplay() = ScoreboardEventManager.getEvent().flatMap { it.getLines() }

    override val configLine = "§7Wide Range of Events\n§7(too much to show all)"
}
