package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.PatcherBeacon
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class PatcherSendCoordinatesHighlight {

    private val patcherBeacon = mutableListOf<PatcherBeacon>()
    private val logger = LorenzLogger("misc/patchercoords")
    private val pattern = "(?<playerName>.*): x: (?<x>.*), y: (?<y>.*), z: (?<z>.*)".toPattern()


    @SubscribeEvent
    fun onPatcherCoordinates(event: LorenzChatEvent) {
        if (!SkyHanniMod.feature.misc.patcherSendCoordWaypoint) return

        val message = event.message.removeColor()
        pattern.matchMatcher(message) {
            val playerName = group("playerName").split(" ").last()
            val x = group("x").toInt()
            val y = group("y").toInt()
            val z = group("z").toInt()
            patcherBeacon.add(PatcherBeacon(LorenzVec(x, y, z),playerName,System.currentTimeMillis()/1000))
            logger.log("got patcher coords and username")
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!SkyHanniMod.feature.misc.patcherSendCoordWaypoint) return

        for (i in 0 until patcherBeacon.size) {
            val location = patcherBeacon[i].loc
            event.drawColor(location, LorenzColor.DARK_GREEN, alpha = 1f)
            event.drawWaypointFilled(location, LorenzColor.GREEN.toColor(), true, true)
            event.drawString(location.add(0.5, 0.5, 0.5), patcherBeacon[i].name, true, LorenzColor.DARK_BLUE.toColor())
        }
    }

    @SubscribeEvent
    fun onEnterWaypoint(event: TickEvent.PlayerTickEvent) {
        val player = event.player
        if(patcherBeacon.size > 0){
            if (player.motionX > 0 || player.motionZ > 0) {
                val location = player.getLorenzVec()
                for (i in 0 until patcherBeacon.size) {
                    logger.log(patcherBeacon[i].time.toString())
                    if(System.currentTimeMillis()/1000 > patcherBeacon[i].time+5){
                        if (location.distanceIgnoreY(patcherBeacon[i].loc) < 5) {
                            patcherBeacon.removeAt(i)
                            logger.log("removed patcher beacon!")
                            break;
                        }
                    }
                }
            }
            if(System.currentTimeMillis()/1000 > patcherBeacon[0].time+60){
                patcherBeacon.removeAt(0)
                logger.log("removed patcher beacon after time!")
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        patcherBeacon.clear()
        logger.log("Reset everything (world change)")
    }
}