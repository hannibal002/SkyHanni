package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.title.TitleManager
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
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// TODO rename to something else to reduce confusion
@SkyHanniModule
object MineshaftWaypoints {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft

    private const val BLOCKS_FORWARD = 7
    private const val BLOCKS_DOWN = -15

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
        if (!config.mineshaftWaypoints.enabled || !config.corpseLocator.enabled) return

        val corpseType = event.corpseType
        val article = if (corpseType == CorpseType.UMBER) "an" else "a"
        ChatUtils.chat("Found $article $corpseType Corpse§e and marked its location with a waypoint.")

        val existingWaypoint = waypoints.find { it.location.distance(event.location) <= 3 }

        if (existingWaypoint != null) {
            existingWaypoint.waypointType = corpseType.waypointType
        } else {
            waypoints.add(
                MineshaftWaypoint(
                    waypointType = corpseType.waypointType,
                    location = event.location.up(),
                    isCorpse = true
                )
            )
        }

        if (event.isLastCorpse && config.corpseLocator.allFoundAlert) {
            TitleManager.sendTitle("§aAll Corpses Found", duration = 3.seconds)
            SoundUtils.playBeepSound()
        }
    }

    @HandleEvent
    fun onCorpseLooted(event: CorpseLootedEvent) {
        if (waypoints.isEmpty()) return

        val closestWaypoint = waypoints.filter { it.isCorpse && it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        closestWaypoint.isLootedCorpse = true
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode != config.shareWaypointLocation) return
        if (timeLastShared.passedSince() < 500.milliseconds) return

        val closestWaypoint = waypoints.filter { it.location.distanceToPlayer() <= 5 }
            .minByOrNull { it.location.distanceToPlayer() } ?: return

        timeLastShared = SimpleTimeMark.now()
        val location = closestWaypoint.location.toChatFormat()
        val type = closestWaypoint.waypointType.display
        val message = "$location | ($type)"

        if (PartyApi.partyMembers.isNotEmpty()) {
            HypixelCommands.partyChat(message)
        } else {
            HypixelCommands.allChat(message)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (waypoints.isEmpty()) return

        waypoints
            .filter {
                (it.isCorpse && config.corpseLocator.enabled) || (!it.isCorpse && config.mineshaftWaypoints.enabled)
            }
            .forEach {
                val waypoint = it.waypointType
                event.drawWaypointFilled(it.location, waypoint.color.toColor(), seeThroughBlocks = true, maximumAlpha = waypoint.maxAlpha)
                event.drawDynamicText(it.location, "${it.colorCode}${waypoint.display}", waypoint.scale)
            }
    }

    private fun addEntranceWaypoints(spawnLocation: LorenzVec, direction: Vec3i) {
        if (config.mineshaftWaypoints.entranceLocation) {
            waypoints.removeIf { it.waypointType == MineshaftWaypointType.ENTRANCE }
            waypoints.add(MineshaftWaypoint(waypointType = MineshaftWaypointType.ENTRANCE, location = spawnLocation))
        }

        if (config.mineshaftWaypoints.ladderLocation) {
            val ladderLocation = spawnLocation
                // Move 7 blocks in front of the player to be in the ladder shaft
                .add(x = direction.x * BLOCKS_FORWARD, z = direction.z * BLOCKS_FORWARD)
                // Adjust 2 blocks to the right to be in the center of the ladder shaft
                .add(x = direction.z * -2, z = direction.x * 2)
                // Move 15 blocks down to be at the bottom of the ladder shaft
                .add(y = BLOCKS_DOWN)

            waypoints.removeIf { it.waypointType == MineshaftWaypointType.LADDER }
            waypoints.add(MineshaftWaypoint(waypointType = MineshaftWaypointType.LADDER, location = ladderLocation))
        }
    }
}
