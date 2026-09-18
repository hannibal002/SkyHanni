package at.hannibal2.skyhanni.features.inventory.tiarelay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TiaRelayWaypoints {

    private val config get() = SkyHanniMod.feature.inventory.helper.tiaRelay
    private var waypoint: LorenzVec? = null
    private var waypointName: String? = null
    private var island = IslandType.NONE

    /**
     * REGEX-TEST: You completed the maintenance on the relay!
     */
    private val completedAllRelaysPattern by RepoPattern.pattern(
        "relay.complete-all",
        "You completed the maintenance on the relay!"
    )

    init {
        Relay.entries.forEach { it.chatPattern }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (!config.nextWaypoint) return

        val message = event.cleanMessage
        Relay.entries.firstOrNull { it.checkChatMessage(message) }?.let { relay ->
            waypoint = relay.waypoint
            waypointName = relay.relayName
            island = relay.island
            return
        }

        if (completedAllRelaysPattern.matches(message)) {
            waypoint = null
            island = IslandType.NONE
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {

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
