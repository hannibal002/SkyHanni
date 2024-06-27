package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.amountOfUnknownLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.unconfirmedUnknownLines

// internal
object Extra : ScoreboardElement() {
    override fun getDisplay(): Any? {
        if (unconfirmedUnknownLines.isEmpty()) return null
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
