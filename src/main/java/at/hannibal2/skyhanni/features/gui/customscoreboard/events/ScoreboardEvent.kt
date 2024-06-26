package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getElementsFromAny
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardLine

abstract class ScoreboardEvent {
    /**
     * Must be specified as one of the following:
     * - `String`
     * - `List<String>`
     * - `ScoreboardLine` (`String align HorizontalAlignment`)
     * - `List<ScoreboardLine>`
     *
     * `null` values will be treated as empty lines/lists.
     */
    protected abstract fun getDisplay(): Any?
    protected open fun showWhen(): Boolean = true
    abstract val configLine: String

    open fun showIsland(): Boolean = true

    fun getLinesOrNull(): List<ScoreboardLine>? = if (showWhen()) getElementsFromAny(getDisplay()).takeIf { it.isNotEmpty() } else null
}
