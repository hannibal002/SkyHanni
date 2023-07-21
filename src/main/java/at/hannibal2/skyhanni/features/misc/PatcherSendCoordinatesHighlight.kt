package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class PatcherSendCoordinatesHighlight {

    private val patcherBeacon = mutableListOf<LorenzVec>()
    private val patcherName = mutableListOf<String>()
    private val logger = LorenzLogger("misc/patchercoords")
    private val pattern = "(?<playerName>.*): x: (?<x>.*), y: (?<y>.*), z: (?<z>.*)".toPattern()

    @SubscribeEvent
    fun onPatcherCoordinates(event: LorenzChatEvent) {
        if (!SkyHanniMod.feature.misc.patcherSendCoordHighlight) return

        val message = event.message.removeColor()
        pattern.matchMatcher(message) {
            val playerName = group("playerName").split(" ").last()
            val x = group("x").toInt()
            val y = group("y").toInt()
            val z = group("z").toInt()

            patcherBeacon.add(LorenzVec(x, y, z))
            patcherName.add(playerName)
            logger.log("got patcher coords and username")
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!SkyHanniMod.feature.misc.patcherSendCoordHighlight) return

        for (i in 0 until patcherBeacon.size) {
            val location = patcherBeacon[i]
            event.drawColor(location, LorenzColor.DARK_GREEN, alpha = 1f)
            event.drawWaypointFilled(location, LorenzColor.GREEN.toColor(), true, true)
            event.drawString(location.add(0.5, 0.5, 0.5), patcherName[i], true, LorenzColor.DARK_BLUE.toColor())
            logger.log("added patcher beacon!")
        }
    }

    @SubscribeEvent
    fun onEnterWaypoint(event: TickEvent.PlayerTickEvent) {
        if (!SkyHanniMod.feature.misc.patcherSendCoordHighlight) return

        val player = event.player
        if (player.motionX > 0 || player.motionZ > 0) {
            val location = player.getLorenzVec()
            for (i in 0 until patcherBeacon.size) {
                if (location.distanceIgnoreY(patcherBeacon[i]) < 5) {
                    patcherBeacon.removeAt(i)
                    patcherName.removeAt(i)
                    logger.log("removed patcher beacon!")
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        patcherBeacon.clear()
        patcherName.clear()
        logger.log("Reset everything (world change)")
    }
}