package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.features.gui.customscoreboard.unconfirmedUnknownLines

// internal
// can just be called from unknown lines handler
object Unknown : ScoreboardElement() {
    override fun getDisplay(): Any? {
        if (unconfirmedUnknownLines.isEmpty()) return null
        return listOf("§cUndetected Lines:") + unconfirmedUnknownLines
    }

    override val configLine = "§cUnknown lines the mod is not detecting"
}
