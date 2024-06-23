package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

abstract class ScoreboardElement {
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

    // TODO: Add Hover and Clickable Feedback to Lines
    //  Suggestion: https://discord.com/channels/997079228510117908/1226508204762992733
}
