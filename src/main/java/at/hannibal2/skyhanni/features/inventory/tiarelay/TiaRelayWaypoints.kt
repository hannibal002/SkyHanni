package at.hannibal2.hanni.features.inventory.tiarelay

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled

@HanniModule
object TiaRelayWaypoints {

    private val config get() = HanniMod.feature.inventory.helper.tiaRelay
    private var waypoint: LorenzVec? = null
    private var waypointName: String? = null
    private var island = IslandType.NONE

    init {
        Relay.entries.forEach { it.chatPattern }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (!config.nextWaypoint) return

        val message = event.message
        Relay.entries.firstOrNull { it.checkChatMessage(message) }?.let { relay ->
            waypoint = relay.waypoint
            waypointName = relay.relayName
            island = relay.island
            return
        }

        if (message == "§aYou completed the maintenance on the relay!") {
            waypoint = null
            island = IslandType.NONE
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: HanniRenderWorldEvent) {

        if (config.allWaypoints) {
            for (relay in Relay.entries) {
                if (relay.island == SkyBlockUtils.currentIsland) {
                    event.drawWaypointFilled(relay.waypoint, LorenzColor.LIGHT_PURPLE.toColor())
                    event.drawDynamicText(relay.waypoint, "§d" + relay.relayName, 1.5)
                }
            }
            return
        }

        if (!config.nextWaypoint) return
        if (SkyBlockUtils.currentIsland != island) return

        waypoint?.let {
            event.drawWaypointFilled(it, LorenzColor.LIGHT_PURPLE.toColor())
            event.drawDynamicText(it, "§d" + waypointName!!, 1.5)
        }
    }
}
