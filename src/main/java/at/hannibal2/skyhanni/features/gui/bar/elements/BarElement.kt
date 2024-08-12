package at.hannibal2.skyhanni.features.gui.bar.elements

abstract class BarElement {
    abstract val configLine: String
    abstract fun getString(): String
}
