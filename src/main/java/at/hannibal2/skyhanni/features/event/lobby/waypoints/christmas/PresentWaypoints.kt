package at.hannibal2.skyhanni.features.event.lobby.waypoints.christmas

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.jsonobjects.repo.EventWaypointsJson
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.event.lobby.waypoints.EventWaypoint
import at.hannibal2.skyhanni.features.event.lobby.waypoints.loadEventWaypoints
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matches
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// todo: create abstract class for this and BasketWaypoints
class PresentWaypoints {
    private val config get() = SkyHanniMod.feature.event.lobbyWaypoints.christmasPresent
    private var presentLocations = mapOf<String, MutableSet<EventWaypoint>>()
    private var presentEntranceLocations = mapOf<String, MutableSet<EventWaypoint>>()
    private var closest: EventWaypoint? = null

    private val presentSet get() = presentLocations[HypixelData.lobbyType]
    private val presentEntranceSet get() = presentEntranceLocations[HypixelData.lobbyType]

    private val presentAlreadyFoundPattern = "§cYou have already found this present!".toPattern()
    private val presentFoundPattern = "§aYou found a.*present! §r§e\\(§r§b\\d+§r§e/§r§b\\d+§r§e\\)".toPattern()
    private val allFoundPattern = "§aCongratulations! You found all the presents in every lobby!".toPattern()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (!isEnabled()) return
        closest = null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        processChatMessage(event.message)
    }

    private fun processChatMessage(message: String) {
        when {
            presentFoundPattern.matches(message) || presentAlreadyFoundPattern.matches(message) -> handlePresentFound()
            allFoundPattern.matches(message) -> handleAllPresentsFound()
        }
    }

    private fun handlePresentFound() {
        presentSet?.minByOrNull { it.position.distanceSqToPlayer() }?.let { present ->
            present.isFound = true
            markEntranceAsFound(present)
            if (closest == present) closest = null
        }
    }

    private fun markEntranceAsFound(present: EventWaypoint) {
        presentEntranceSet?.find { present.name == it.name }?.let { it.isFound = true }
    }

    private fun handleAllPresentsFound() {
        // If all presents are found, disable the feature
        LorenzUtils.chat("Congratulations! As all presents are found, we are disabling the Christmas Present Waypoints feature.")
        config.allWaypoints = false
        config.allEntranceWaypoints = false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled() && config.onlyClosest && HypixelData.locrawData != null && closest == null) return
        val notFoundPresents = presentSet?.filterNot { it.isFound }
        if (notFoundPresents?.isEmpty() == true) return
        closest = notFoundPresents?.minByOrNull { it.position.distanceSqToPlayer() } ?: return
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        presentSet?.let { event.drawWaypoints(it, config.allWaypoints, LorenzColor.GOLD, "§6") }
        presentEntranceSet?.let { event.drawWaypoints(it, config.allEntranceWaypoints, LorenzColor.YELLOW, "§e") }
    }

    private fun LorenzRenderWorldEvent.drawWaypoints(
        waypoints: Set<EventWaypoint>, shouldDraw: Boolean, color: LorenzColor, prefix: String
    ) {
        if (!shouldDraw) return
        waypoints.forEach { waypoint ->
            if (!waypoint.shouldShow()) return@forEach
            this.drawWaypointFilled(waypoint.position, color.toColor())
            this.drawDynamicText(waypoint.position, "$prefix${waypoint.name}", 1.5)
        }
    }

    private fun EventWaypoint.shouldShow(): Boolean = !isFound && (!config.onlyClosest || closest == this)

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EventWaypointsJson>("EventWaypoints")
        presentLocations = loadEventWaypoints(data.presents ?: error("'presents' is null in EventWaypoints!"))
        presentEntranceLocations =
            loadEventWaypoints(data.presents_entrances ?: error("'presents_entrances' is null in EventWaypoints!"))
    }

    private fun isEnabled(): Boolean =
        LorenzUtils.onHypixel && HypixelData.inLobby && (config.allWaypoints || config.allEntranceWaypoints)
}
