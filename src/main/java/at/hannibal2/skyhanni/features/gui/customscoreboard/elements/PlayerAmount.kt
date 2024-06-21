package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData.getMaxPlayersForCurrentServer
import at.hannibal2.skyhanni.data.HypixelData.getPlayersOnCurrentServer
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig

object PlayerAmount : Element() {
    override fun getDisplayPair(): List<Any> = buildList {
        val max = if (displayConfig.showMaxIslandPlayers) {
            "§7/§a${getMaxPlayersForCurrentServer()}"
        } else ""
        if (displayConfig.displayNumbersFirst) {
            add("§a${getPlayersOnCurrentServer()}$max Players")
        } else add("§7Players: §a${getPlayersOnCurrentServer()}$max")
    }

    override fun showWhen() = true

    override val configLine = "§7Players: §a69§7/§a80"
}
