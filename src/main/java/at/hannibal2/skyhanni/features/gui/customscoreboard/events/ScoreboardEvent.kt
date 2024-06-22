package at.hannibal2.skyhanni.features.gui.customscoreboard.events

abstract class ScoreboardEvent {
    abstract fun getDisplay(): List<Any>
    abstract fun showWhen(): Boolean
    abstract val configLine: String
}
