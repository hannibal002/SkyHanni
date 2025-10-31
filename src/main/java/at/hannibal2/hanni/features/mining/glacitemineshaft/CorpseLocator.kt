package at.hannibal2.hanni.features.mining.glacitemineshaft

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.PartyApi
import at.hannibal2.hanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.hanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.LocationUtils.canBeSeen
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.PlayerUtils
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.compat.getStandHelmet
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand

// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
@HanniModule
object CorpseLocator {
    private val config get() = HanniMod.feature.mining.glaciteMineshaft.corpseLocator

    /**
     * REGEX-TEST: x: -164, y: 8, z: -154 | (Lapis Corpse)
     * REGEX-TEST: x: 141, y: 14, z: -131
     * REGEX-TEST: x: -9, y: 135, z: 20 | (Tungsten Corpse)
     */
    private val mineshaftCoordsPattern by RepoPattern.pattern(
        "mineshaft.corpse.coords",
        "x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)(?:.+)?",
    )

    private val sharedWaypoints: MutableList<LorenzVec> = mutableListOf()

    private fun findCorpse() {
        EntityUtils.getAllEntities().filterIsInstance<EntityArmorStand>()
            .filterNot { corpse -> MineshaftWaypoints.waypoints.any { it.location.distance(corpse.getLorenzVec()) <= 3 } }
            .filter { entity ->
                entity.showArms && entity.hasNoBasePlate() && !entity.isInvisible
            }
            .forEach { entity ->
                val helmetName = entity.getStandHelmet()?.getInternalName() ?: return
                val corpseType = MineshaftWaypointType.getByHelmetOrNull(helmetName) ?: return

                val canSee = entity.getLorenzVec().canBeSeen(-1..3)
                if (canSee) {
                    val article = if (corpseType.displayText == "Umber Corpse") "an" else "a"
                    ChatUtils.chat("Located $article ${corpseType.displayText} and marked its location with a waypoint.")

                    MineshaftWaypoints.waypoints.add(
                        MineshaftWaypoint(
                            waypointType = corpseType,
                            location = entity.getLorenzVec().up(),
                            isCorpse = true,
                        ),
                    )
                }
            }
    }

    private fun shareCorpse() {
        val closestCorpse = MineshaftWaypoints.waypoints.filter { it.isCorpse && !it.shared }
            .filterNot { corpse ->
                sharedWaypoints.any { corpse.location.distance(it) <= 5 }
            }
            .filter { it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        val (x, y, z) = closestCorpse.location.toDoubleArray().map { it.toInt() }
        val type = closestCorpse.waypointType.displayText

        HypixelCommands.partyChat("x: $x, y: $y, z: $z | ($type)")
        closestCorpse.shared = true
    }


    @HandleEvent
    fun onWorldChange() {
        sharedWaypoints.clear()
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        findCorpse()

        if (!config.autoSendLocation) return
        if (MineshaftWaypoints.waypoints.isEmpty()) return
        if (PartyApi.partyMembers.isEmpty()) return
        shareCorpse()
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent) {
        handleChatEvent(event.author, event.message)
    }

    @HandleEvent
    fun onAllChat(event: PlayerAllChatEvent) {
        handleChatEvent(event.author, event.message)
    }

    private fun handleChatEvent(author: String, message: String) {
        if (!isEnabled()) return
        if (PlayerUtils.getName() in author) return

        mineshaftCoordsPattern.matchMatcher(message) {
            val (x, y, z) = listOf(group("x"), group("y"), group("z")).map { it.formatInt() }
            val location = LorenzVec(x, y, z)

            // Return if someone had already sent a location nearby
            if (sharedWaypoints.any { it.distance(location) <= 5 }) return
            sharedWaypoints.add(location)
        }
    }

    fun isEnabled() = IslandType.MINESHAFT.isCurrent() && config.enabled
}
