package at.hannibal2.skyhanni.features.gui.customscoreboard.events

abstract class ScoreboardEvent {
    /**
     * Must be specified as one of the following:
     * - `String`
     * - `List<String>`
     * - `ScoreboardElementType` (`Pair<String, HorizontalAlignment>`)
     * - `List<ScoreboardElementType>` (`List<Pair<String, HorizontalAlignment>>`)
     *
     * `null` values will be treated as empty lines.
     */
    abstract fun getDisplay(): Any?
    open fun showWhen(): Boolean = true
    abstract val configLine: String

    open fun showIsland(): Boolean = true
}
