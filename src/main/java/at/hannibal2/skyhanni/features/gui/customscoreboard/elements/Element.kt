package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

abstract class Element {
    abstract fun getDisplayPair(): Any
    abstract fun showWhen(): Boolean
}
