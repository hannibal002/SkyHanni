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
        val lobbyCode = HypixelData.serverId?.let { "§8$it" }.orEmpty()
        val roomId = DungeonAPI.getRoomID()?.let { " §8$it" }.orEmpty()
        val localDate = LocalDate.now().format(formatter)

        return buildString {
            if (CustomScoreboard.displayConfig.dateInLobbyCode) append("§7$localDate ")
            append(lobbyCode)
            append(roomId)
        }
    }

    override val configLine = "§710/23/2024 §8mega77CK"
}
