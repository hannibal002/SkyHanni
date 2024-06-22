package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI

object LobbyCode : ScoreboardElement() {
    override fun getDisplay(): String? {
        val lobbyCode = HypixelData.serverId ?: return null
        val roomId = DungeonAPI.getRoomID()?.let { " §8$it" } ?: ""
        return "§8$lobbyCode$roomId"
    }

    override val configLine = "§8mega77CK"
}
