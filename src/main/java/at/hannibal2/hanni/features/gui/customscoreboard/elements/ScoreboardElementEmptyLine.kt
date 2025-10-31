package at.hannibal2.hanni.features.gui.customscoreboard.elements

// none
// doesn't need to ever update
object ScoreboardElementEmptyLine : ScoreboardElement() {
    override fun getDisplay() = ""

    override val configLine = "----- Separator -----"
}
