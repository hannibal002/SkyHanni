package at.hannibal2.hanni.features.gui.customscoreboard

import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.hanni.utils.RenderUtils

data class ScoreboardLine(
    val display: String,
    val alignment: RenderUtils.HorizontalAlignment = DEFAULT_ALIGNMENT,
) {

    companion object {
        private val DEFAULT_ALIGNMENT get() = displayConfig.textAlignment

        fun String.align(): ScoreboardLine = ScoreboardLine(this, DEFAULT_ALIGNMENT)

        infix fun String.align(alignment: RenderUtils.HorizontalAlignment): ScoreboardLine = ScoreboardLine(this, alignment)
    }
}
