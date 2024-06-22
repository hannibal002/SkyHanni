package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.WinterAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches

object Winter : ScoreboardEvent() {
    override fun getDisplay() = listOf(
        ScoreboardPattern.winterEventStartPattern,
        ScoreboardPattern.winterNextWavePattern,
        ScoreboardPattern.winterWavePattern,
        ScoreboardPattern.winterMagmaLeftPattern,
        ScoreboardPattern.winterTotalDmgPattern,
        ScoreboardPattern.winterCubeDmgPattern,
    ).allMatches(getSbLines()).filter { !it.endsWith("Soon!") }

    override fun showWhen() = WinterAPI.inWorkshop()

    override val configLine = "§7(All Winter Event Lines)"
}
