package at.hannibal2.skyhanni.features.gui.customscoreboard.events

abstract class ScoreboardEvent {
    /**
     * Must be specified as one of the following:
     * - `String`
     * - `List<String>`
     * - `ScoreboardLine` (`String align HorizontalAlignment`)
     * - `List<ScoreboardLine>`
     *
     * `null` values will be treated as empty lines.
     */
    abstract fun getDisplay(): Any?
    open fun showWhen(): Boolean = true
    abstract val configLine: String

    open fun showIsland(): Boolean = true
}
