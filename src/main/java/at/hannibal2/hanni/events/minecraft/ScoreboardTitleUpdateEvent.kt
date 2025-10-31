package at.hannibal2.hanni.events.minecraft

import at.hannibal2.hanni.api.event.HanniEvent

class ScoreboardTitleUpdateEvent(val title: String, objectiveName: String) : HanniEvent() {
    // The objective name is different depending on what gamemode you are playing in hypixel, or in what lobby.
    // In SkyWars its 'skyLobby' in the main lobby its 'MainScoreboard', in BedWars and prototype lobby its 'Prototype', and so on.
    // When in SkyBlock, it is called 'SBScoreboard'.
    val isSkyblock: Boolean = objectiveName == "SBScoreboard"
}
