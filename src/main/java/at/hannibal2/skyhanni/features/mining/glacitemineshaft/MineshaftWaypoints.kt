package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.events.mining.CorpseFoundEvent
import at.hannibal2.skyhanni.events.mining.CorpseLootedEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse.CorpseType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import kotlin.time.Duration.Companion.milliseconds

// TODO rename to something else to reduce confusion
@SkyHanniModule
object MineshaftWaypoints {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.waypointsConfig

    private const val LADDER_BLOCKS_FORWARD = 7
    private const val LADDER_BLOCKS_DOWN = -15

    val waypoints = mutableListOf<MineshaftWaypoint>()
    private var timeLastShared = SimpleTimeMark.farPast()
    private var isWorldLoaded = false

    @HandleEvent
    fun onWorldChange() {
        waypoints.clear()
        isWorldLoaded = false
    }

    @HandleEvent
    fun onIslandJoin(event: IslandJoinEvent) {
        if (event.island != IslandType.MINESHAFT) return

        val spawnLocation = LocationUtils.getBlockBelowPlayer()
        val direction = MinecraftCompat.localPlayerOrThrow.direction.unitVec3i

        addEntranceWaypoints(spawnLocation, direction)
    }

    @HandleEvent(onlyOnIsland = IslandType.MINESHAFT)
    fun onPacketReceived(event: PacketReceivedEvent) {
        if (isWorldLoaded) return

        when (event.packet) {
            is ClientboundLevelChunkWithLightPacket -> isWorldLoaded = true
            is ClientboundPlayerPositionPacket -> {
                if (event.packet.relatives.isNotEmpty()) return

                val spawnLocation = event.packet.change.position.toLorenzVec().add(y = -1).roundToBlock()
                val direction = Direction.fromYRot(event.packet.change.yRot.toDouble()).unitVec3i

                addEntranceWaypoints(spawnLocation, direction)
            }
        }
    }

    @HandleEvent
    fun onCorpseFound(event: CorpseFoundEvent) {
        val waypoint = MineshaftWaypoint(event.corpseType.waypointType, event.location)
        waypoints.add(waypoint)

        // Only display a message when Found Corpse waypoints are enabled.
        if (config.types.foundCorpse) {
            val article = if (event.corpseType == CorpseType.UMBER) "an" else "a"
            ChatUtils.chat("Found $article ${event.corpseType} Corpse§e at ${event.location.toLocalFormat()}.")
        }
    }

    @HandleEvent
    fun onCorpseLooted(event: CorpseLootedEvent) {
        val closestWaypoint = waypoints.filter { it.type.isCorpse && it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        closestWaypoint.isLootedCorpse = true
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (MinecraftCompat.screen != null) return
        if (event.keyCode != config.shareFoundCorpseKeybind) return
        if (timeLastShared.passedSince() < 500.milliseconds) return

        val closestWaypoint = waypoints.filter { it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        timeLastShared = SimpleTimeMark.now()
        val location = closestWaypoint.location.toChatFormat()
        val type = closestWaypoint.type.display
        val message = "$location | ($type)"

        if (PartyApi.partyMembers.isNotEmpty()) {
            HypixelCommands.partyChat(message)
        } else {
            HypixelCommands.allChat(message)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        waypoints
            .filter { it.type.renderCondition() }
            .forEach {
                val color = (if (it.isLootedCorpse) LorenzColor.GREEN else LorenzColor.YELLOW).getChatColor()
                event.drawWaypointFilled(it.location, it.type.color.toColor(), seeThroughBlocks = true)
                event.drawDynamicText(it.location, "$color${it.type.display}", 1.0)
            }
    }

    private fun addEntranceWaypoints(entranceLocation: LorenzVec, direction: Vec3i) {
        waypoints.removeIf { it.type in listOf(MineshaftWaypointType.ENTRANCE, MineshaftWaypointType.LADDER) }

        val ladderLocation = entranceLocation
            // Move 7 blocks in front of the player to be in the ladder shaft
            .add(x = direction.x * LADDER_BLOCKS_FORWARD, z = direction.z * LADDER_BLOCKS_FORWARD)
            // Adjust 2 blocks to the right to be in the center of the ladder shaft
            .add(x = direction.z * -2, z = direction.x * 2)
            // Move 15 blocks down to be at the bottom of the ladder shaft
            .add(y = LADDER_BLOCKS_DOWN)

        waypoints.add(MineshaftWaypoint(type = MineshaftWaypointType.ENTRANCE, location = entranceLocation))
        waypoints.add(MineshaftWaypoint(type = MineshaftWaypointType.LADDER, location = ladderLocation))
    }
}
