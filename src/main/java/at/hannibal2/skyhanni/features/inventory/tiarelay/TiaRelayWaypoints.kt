package at.hannibal2.skyhanni.features.inventory.tiarelay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TiaRelayWaypoints {

    private val config get() = SkyHanniMod.feature.inventory.helper.tiaRelay
    private var waypoint: LorenzVec? = null
    private var waypointName: String? = null
    private var island = IslandType.NONE

    init {
        Relay.entries.forEach { it.chatPattern }
    }

    private val patternGroup = RepoPattern.group("tiarelaywaypoints")
    private val messagePattern by patternGroup.pattern(
        "message",
        "§aYou completed the maintenance on the relay!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.nextWaypoint) return

        val message = event.message
        Relay.entries.firstOrNull { it.checkChatMessage(message) }?.let { relay ->
            waypoint = relay.waypoint
            waypointName = relay.relayName
            island = relay.island
            return
        }

        if (messagePattern.matches(message)) {
            waypoint = null
            island = IslandType.NONE
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (config.allWaypoints) {
            for (relay in Relay.entries) {
                if (relay.island == LorenzUtils.skyBlockIsland) {
                    event.drawWaypointFilled(relay.waypoint, LorenzColor.LIGHT_PURPLE.toColor())
                    event.drawDynamicText(relay.waypoint, "§d" + relay.relayName, 1.5)
                }
            }
            return
        }

        if (!config.nextWaypoint) return
        if (LorenzUtils.skyBlockIsland != island) return

        waypoint?.let {
            event.drawWaypointFilled(it, LorenzColor.LIGHT_PURPLE.toColor())
            event.drawDynamicText(it, "§d" + waypointName!!, 1.5)
        }
    }
}
