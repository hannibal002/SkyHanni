package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGems
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Gems : Element() {
    override fun getDisplayPair(): List<Any> {
        val gems = getGems()

        return listOf(
            when {
                informationFilteringConfig.hideEmptyLines && gems == "0" -> HIDDEN
                displayConfig.displayNumbersFirst -> "§a$gems Gems"
                else -> "Gems: §a$gems"
            },
        )
    }

    override fun showWhen() = !inAnyIsland(IslandType.THE_RIFT, IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)

    override val configLine = "Gems: §a57,873"
}
