package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

object MineshaftWaypoints {
    private val config get() = SkyHanniMod.feature.mining.mineshaftWaypoints

    private const val blocksForward: Int = 7

    val waypoints: MutableList<Waypoint> = mutableListOf()
    private var timeLastShared = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        waypoints.clear()
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.MINESHAFT) return

        val playerLocation = LocationUtils.playerLocation().round(0).add(y = -1)

        if (config.entranceLocation) {
            waypoints.add(Waypoint(waypointType = MineshaftWaypointType.ENTRANCE, location = playerLocation, shared = true))
        }

        if (config.ladderLocation) {
            val waypointType = MineshaftWaypointType.LADDER
            val vec = Minecraft.getMinecraft().thePlayer.horizontalFacing.directionVec
            val location = playerLocation
                // Move 7 blocks in front of the player to be in the ladder shaft
                .add(x = vec.x * blocksForward, y = -15, z = vec.z * blocksForward)
                // Adjust 2 blocks to the right to be in the center of the ladder shaft
                .add(x = vec.z * -2, z = vec.x * 2)
            waypoints.add(Waypoint(waypointType = waypointType, location = location, shared = true))
        }
    }

    @SubscribeEvent
    fun onKeyPress(event: LorenzKeyPressEvent) {
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (event.keyCode != config.shareWaypointLocation) return
        if (timeLastShared.passedSince() < 500.milliseconds) return

        waypoints.filter { it.location.distanceToPlayer() <= 5 }
            .forEach {
                timeLastShared = SimpleTimeMark.now()
                val location = it.location
                val (x, y, z) = location.toDoubleArray().map { it.toInt() }
                val type = it.waypointType.displayText

                val messagePrefix = if (PartyAPI.partyMembers.isEmpty()) "" else "/pc"
                ChatUtils.sendMessageToServer("$messagePrefix x: $x, y: $y, z: $z | ($type)")
            }
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!IslandType.MINESHAFT.isInIsland()) return
        if (waypoints.isEmpty()) return

        waypoints.forEach {
            event.drawWaypointFilled(it.location, it.waypointType.color.toColor(), seeThroughBlocks = true)
            event.drawDynamicText(it.location, "§e${it.waypointType.displayText}", 1.0)
        }
    }
}
