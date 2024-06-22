package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN

object LobbyCode : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        val lobbyCode = HypixelData.serverId
        val roomId = DungeonAPI.getRoomID()?.let { "§8$it" } ?: ""
        val lobbyDisplay = lobbyCode?.let { "§8$it $roomId" } ?: HIDDEN
        return listOf(lobbyDisplay)
    }

    override fun showWhen() = true

    override val configLine = "§8mega77CK"
}
