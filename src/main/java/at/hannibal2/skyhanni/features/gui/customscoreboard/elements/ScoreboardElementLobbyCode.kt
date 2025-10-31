package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.data.DateFormat
import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.data.MiningApi
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern

// internal
// update on island change and every second while in dungeons
object ScoreboardElementLobbyCode : ScoreboardElement() {
    override fun getDisplay() = buildString {
        if (CustomScoreboard.displayConfig.dateInLobbyCode) append("§7${CustomScoreboard.displayConfig.dateFormat}")
        listOfNotNull(
            HypixelData.serverId,
            DungeonApi.roomId,
            MiningApi.mineshaftRoomId,
        ).distinct().forEach { append(" §8$it") }
    }.trim()

    override val configLine = "§7${DateFormat.US_SLASH_MMDDYYYY} §8mega77CK"

    override val elementPatterns = listOf(ScoreboardPattern.lobbyCodePattern)
}
