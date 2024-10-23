package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// internal
// update on island change and every second while in dungeons
object ScoreboardElementLobbyCode : ScoreboardElement() {
    private val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

    override fun getDisplay(): String? {
        val lobbyCode = HypixelData.serverId
        val roomId = DungeonAPI.getRoomID()?.let { " §8$it" }.orEmpty()
        val localDate = LocalDate.now().format(formatter)

        return (if (CustomScoreboard.displayConfig.dateInLobbyCode) "§7$localDate " else "") + lobbyCode?.let { "§8$it" } + roomId
    }

    override val configLine = "§710/23/2024 §8mega77CK"
}
