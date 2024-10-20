package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraAPI
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventKuudra : ScoreboardEvent() {

    private val patterns = listOf(
        ScoreboardPattern.autoClosingPattern,
        ScoreboardPattern.startingInPattern,
        ScoreboardPattern.timeElapsedPattern,
        ScoreboardPattern.instanceShutdownPattern,
        ScoreboardPattern.wavePattern,
        ScoreboardPattern.tokensPattern,
        ScoreboardPattern.submergesPattern,
    )

    override fun getDisplay() = patterns.allMatches(getSbLines())

    override val configLine = "§7(All Kuudra Lines)"

    override fun showIsland() = KuudraAPI.inKuudra()
}
