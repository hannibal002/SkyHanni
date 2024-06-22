package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object GardenCleanUp : ScoreboardEvent() {
    override fun getDisplay() = ScoreboardPattern.cleanUpPattern.firstMatches(getSbLines())?.trim()

    override val configLine = "Cleanup: §c12.6%"

    override fun showIsland() = GardenAPI.inGarden()
}
