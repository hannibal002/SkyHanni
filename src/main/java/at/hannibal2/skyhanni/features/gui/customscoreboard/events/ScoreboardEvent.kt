package at.hannibal2.skyhanni.features.gui.customscoreboard.events

abstract class ScoreboardEvent {
    abstract fun getDisplay(): List<Any>
    open fun showWhen(): Boolean = true
    abstract val configLine: String

    open fun showIsland(): Boolean = true
}
