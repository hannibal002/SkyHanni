package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.features.gui.customscoreboard.amountOfUnknownLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.unconfirmedUnknownLines

object Extra : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        if (unconfirmedUnknownLines.isEmpty()) return listOf(HIDDEN)
        amountOfUnknownLines = unconfirmedUnknownLines.size

        return listOf("§cUndetected Lines:") + unconfirmedUnknownLines
    }

    override fun showWhen(): Boolean {
        if (unconfirmedUnknownLines.isEmpty()) {
            amountOfUnknownLines = 0
        }
        return unconfirmedUnknownLines.isNotEmpty()
    }

    override val configLine = "§cUnknown lines the mod is not detecting"
}
