package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

abstract class Element {
    abstract fun getDisplay(): List<Any>
    abstract fun showWhen(): Boolean
    abstract val configLine: String

    // TODO: Add Hover and Clickable Feedback to Lines
    // Suggestion: https://discord.com/channels/997079228510117908/1226508204762992733
}
