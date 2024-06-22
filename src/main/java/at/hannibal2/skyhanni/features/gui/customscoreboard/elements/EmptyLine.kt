package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.EMPTY

object EmptyLine : ScoreboardElement() {
    override fun getDisplay() = listOf(EMPTY)

    override val configLine = ""
}
