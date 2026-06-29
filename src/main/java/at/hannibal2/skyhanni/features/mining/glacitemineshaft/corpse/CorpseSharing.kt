package at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftWaypoints
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.toLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CorpseSharing {

    /**
     * REGEX-TEST: x: -164, y: 8, z: -154 | (Lapis Corpse)
     * REGEX-TEST: x: 141, y: 14, z: -131
     * REGEX-TEST: x: -9, y: 135, z: 20 | (Tungsten Corpse)
     */
    private val mineshaftCoordsPattern by RepoPattern.pattern(
        "mineshaft.corpse.coords",
        "x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)(?:.+)?",
    )

    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.corpseLocator

    // This list only keeps track of already shared waypoints by anyone in the chat.
    // They don't get rendered.
    private val sharedWaypoints = mutableListOf<LorenzVec>()

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onSecondPassed() {
        if (!config.autoSendLocation) return
        if (MineshaftWaypoints.waypoints.isEmpty()) return
        if (PartyApi.partyMembers.isEmpty()) return
        shareCorpse()
    }

    private fun shareCorpse() {
        val closestCorpse = MineshaftWaypoints.waypoints.filter { it.isCorpse && !it.shared }
            .filterNot { corpse ->
                sharedWaypoints.any { corpse.location.distance(it) <= 5 }
            }
            .filter { it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        val location = closestCorpse.location.toChatFormat()
        val type = closestCorpse.waypointType.display

        HypixelCommands.partyChat("$location | ($type)")
        closestCorpse.shared = true
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent.Allow) {
        handleChatEvent(event.author, event.message)
    }

    @HandleEvent
    fun onAllChat(event: PlayerAllChatEvent.Allow) {
        handleChatEvent(event.author, event.message)
    }

    @HandleEvent
    fun onWorldChange() {
        sharedWaypoints.clear()
    }

    private fun handleChatEvent(author: String, message: String) {
        if (!config.enabled || !IslandType.MINESHAFT.isInIsland()) return
        if (PlayerUtils.getName() in author) return

        mineshaftCoordsPattern.matchMatcher(message) {
            val location = toLorenzVec() ?: return

            // Return if someone had already sent a location nearby
            if (sharedWaypoints.any { it.distance(location) <= 5 }) return
            sharedWaypoints.add(location)
        }
    }
}
