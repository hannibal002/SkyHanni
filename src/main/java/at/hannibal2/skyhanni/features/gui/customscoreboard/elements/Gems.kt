package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGems
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Gems : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        val gems = getGems()

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && gems == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "§a$gems Gems"
                else -> "Gems: §a$gems"
            },
        )
    }

    override val configLine = "Gems: §a57,873"

    override fun showIsland() = !inAnyIsland(IslandType.THE_RIFT, IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)
}
