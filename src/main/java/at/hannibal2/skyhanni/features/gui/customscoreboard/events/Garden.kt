package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Garden : ScoreboardEvent() {
    override fun getDisplay() = buildList {
        addNotNull(ScoreboardPattern.pastingPattern.firstMatches(getSbLines())?.trim())
        addNotNull(ScoreboardPattern.cleanUpPattern.firstMatches(getSbLines())?.trim())
    }

    override val configLine = "§7(All Garden Lines)"

    override fun showIsland() = GardenAPI.inGarden()
}
