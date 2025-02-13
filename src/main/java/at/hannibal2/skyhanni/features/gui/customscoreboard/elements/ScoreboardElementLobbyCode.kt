package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.DateFormat
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern

// internal
// update on island change and every second while in dungeons
object ScoreboardElementLobbyCode : ScoreboardElement() {
    override fun getDisplay() = buildString {
        if (CustomScoreboard.displayConfig.dateInLobbyCode) append("§7${CustomScoreboard.displayConfig.dateFormat}")
        listOfNotNull(
            HypixelData.serverId,
            DungeonApi.roomId,
            "test string",
            MiningApi.mineshaftRoomId,
        ).forEach { append(" §8$it") }
    }.trim()

    override val configLine = "§7${DateFormat.US_SLASH_MMDDYYYY} §8mega77CK"

    override val elementPatterns = listOf(ScoreboardPattern.lobbyCodePattern)
}
