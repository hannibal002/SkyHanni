package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.EMPTY

object EmptyLine : Element() {
    override fun getDisplay() = listOf(EMPTY)

    override fun showWhen() = true

    override val configLine = ""
}
