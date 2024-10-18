package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData.getMaxPlayersForCurrentServer
import at.hannibal2.skyhanni.data.HypixelData.getPlayersOnCurrentServer
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig

// tablist
// tablist update event (maybe possible with widgets)
object ScoreboardElementPlayerAmount : ScoreboardElement() {
    override fun getDisplay(): String {
        val max = if (displayConfig.showMaxIslandPlayers) {
            "§7/§a${getMaxPlayersForCurrentServer()}"
        } else ""
        return if (displayConfig.displayNumbersFirst) {
            "§a${getPlayersOnCurrentServer()}$max Players"
        } else "§7Players: §a${getPlayersOnCurrentServer()}$max"
    }

    override val configLine = "§7Players: §a69§7/§a80"
}
