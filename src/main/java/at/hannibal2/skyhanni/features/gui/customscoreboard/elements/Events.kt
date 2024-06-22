package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEvent

object Events : Element() {
    override fun getDisplay() = ScoreboardEvent.getEvent().flatMap { it.getLines() }

    override fun showWhen() = true

    override val configLine = "§7Wide Range of Events\n§7(too much to show all)"
}
