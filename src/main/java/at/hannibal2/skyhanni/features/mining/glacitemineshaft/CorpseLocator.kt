package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.toLorenzVec
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.seconds

// TODO: Maybe implement automatic warp-in for chosen players if the user is not in a party.
@SkyHanniModule
object CorpseLocator {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.corpseLocator

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

    private var foundAllCorpses = false

    @OptIn(AllEntitiesGetter::class)
    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onTick() {
        if (!isEnabled() || foundAllCorpses) return

        val entities = EntityUtils.getAllEntities().filterIsInstance<ArmorStand>()

        val corpsesFound = MineshaftWaypoints.waypoints.count {
            it.isCorpse && (it.isLootedCorpse || it.waypointType != MineshaftWaypointType.POTENTIAL)
        }

        for (entity in entities) {
            if (!entity.showArms() || entity.showBasePlate() || entity.isInvisible) continue

            val helmetName = entity.getStandHelmet()?.getInternalName()
            val corpseType = helmetName?.let(MineshaftWaypointType::getByHelmetOrNull) ?: continue

            val corpsePos = entity.getLorenzVec()
            if (
                MineshaftWaypoints.waypoints.any {
                    it.waypointType != MineshaftWaypointType.POTENTIAL &&
                        it.location.distance(corpsePos) <= 3
                } ||
                !corpsePos.canBeSeen(-1..3)
            ) continue

            val article = if (corpseType.displayText == "Umber Corpse") "an" else "a"
            ChatUtils.chat("Located $article ${corpseType.displayText} and marked its location with a waypoint.")

            val existing = MineshaftWaypoints.waypoints.find { it.location.distance(corpsePos) <= 3 }

            if (existing != null) {
                existing.waypointType = corpseType
            } else {
                MineshaftWaypoints.waypoints.add(
                    MineshaftWaypoint(
                        waypointType = corpseType,
                        location = corpsePos.up(),
                        isCorpse = true
                    )
                )
            }
        }

        checkAllCorpsesFound(entities, corpsesFound)
    }

    private fun checkAllCorpsesFound(entities: Sequence<ArmorStand>, alreadyFoundAmount: Int) {
        val totalCorpseAmount = TabWidget.FROZEN_CORPSES.lines.size - 1
        val newFoundAmount = MineshaftWaypoints.waypoints.count {
            it.isCorpse && it.waypointType != MineshaftWaypointType.POTENTIAL
        }
        foundAllCorpses = totalCorpseAmount == newFoundAmount

        if (foundAllCorpses && newFoundAmount > alreadyFoundAmount && config.allFoundAlert) {
            TitleManager.sendTitle("§aAll Corpses Found", duration = 3.seconds)
            SoundUtils.playBeepSound()
        }

        MineshaftWaypoints.waypoints.removeIf { waypoint ->
            if (waypoint.waypointType != MineshaftWaypointType.POTENTIAL) return@removeIf false
            if (foundAllCorpses) return@removeIf true
            if (!waypoint.location.canBeSeen(-1..3)) return@removeIf false

            entities.none { waypoint.location.distance(it.getLorenzVec()) <= 3 }
        }
    }

    private fun shareCorpse() {
        val closestCorpse = MineshaftWaypoints.waypoints.filter { it.isCorpse && !it.shared }
            .filterNot { corpse ->
                sharedWaypoints.any { corpse.location.distance(it) <= 5 }
            }
            .filter { it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        val location = closestCorpse.location.toChatFormat()
        val type = closestCorpse.waypointType.displayText

        HypixelCommands.partyChat("$location | ($type)")
        closestCorpse.shared = true
    }


    @HandleEvent
    fun onWorldChange() {
        sharedWaypoints.clear()
        foundAllCorpses = false
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!config.autoSendLocation) return
        if (MineshaftWaypoints.waypoints.isEmpty()) return
        if (PartyApi.partyMembers.isEmpty()) return
        shareCorpse()
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent.Allow) {
        handleChatEvent(event.author, event.message)
    }

    @HandleEvent
    fun onAllChat(event: PlayerAllChatEvent.Allow) {
        handleChatEvent(event.author, event.message)
    }

    private fun handleChatEvent(author: String, message: String) {
        if (!isEnabled()) return
        if (PlayerUtils.getName() in author) return

        mineshaftCoordsPattern.matchMatcher(message) {
            val location = toLorenzVec() ?: return

            // Return if someone had already sent a location nearby
            if (sharedWaypoints.any { it.distance(location) <= 5 }) return
            sharedWaypoints.add(location)
        }
    }

    fun isEnabled() = IslandType.MINESHAFT.isInIsland() && config.enabled
}
