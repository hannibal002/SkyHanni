package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

// none
// doesn't need to ever update
object EmptyLine : ScoreboardElement() {
    override fun getDisplay() = ""

    override val configLine = ""
}
