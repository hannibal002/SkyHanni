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
        val message = event.message

        if (presentFoundPattern.matches(message) || presentAlreadyFoundPattern.matches(message)) {
            val present = presentSet?.minByOrNull() { it.position.distanceSqToPlayer() } ?: return
            present.isFound = true
            presentEntranceSet?.find { present.name == it.name }?.let {
                it.isFound = true
            }
            if (closest == present) {
                closest = null
            }
            return
        }

        // If all presents are found, disable the feature
        if (allFoundPattern.matches(message)) {
            LorenzUtils.chat("Congratulations! As all presents are found, we are disabling the Christmas Present Waypoints feature.")
            config.allWaypoints = false
            config.allEntranceWaypoints = false
            return
        }

    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (config.onlyClosest && HypixelData.locrawData != null && closest == null) {
            val notFoundPresents = presentSet?.filter { !it.isFound }
            if (notFoundPresents?.isEmpty() == true) return
            closest = notFoundPresents?.minByOrNull { it.position.distanceSqToPlayer() } ?: return
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        val presentSetTemp = presentSet ?: return

        if (config.allWaypoints) {
            presentSetTemp.forEach {
                if (!it.shouldShow()) return@forEach
                event.drawWaypointFilled(it.position, LorenzColor.GOLD.toColor())
                event.drawDynamicText(it.position, "§6" + it.name, 1.5)
            }
        }

        if (config.allEntranceWaypoints) {
            presentEntranceSet?.forEach {
                if (!it.shouldShow()) return@forEach
                event.drawWaypointFilled(it.position, LorenzColor.YELLOW.toColor())
                event.drawDynamicText(it.position, "§e" + it.name, 1.5)
            }
        }
    }

    private fun EventWaypoint.shouldShow(): Boolean {
        return !isFound && (!config.onlyClosest || closest == this)
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EventWaypointsJson>("EventWaypoints")
        val presents = data.presents ?: error("'presents' is null in EventWaypoints!")
        presentLocations = loadEventWaypoints(presents)

        val presentEntrances = data.presents_entrances ?: error("'presents_entrances' is null in EventWaypoints!")
        presentEntranceLocations = loadEventWaypoints(presentEntrances)
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.onHypixel && HypixelData.inLobby && (config.allWaypoints || config.allEntranceWaypoints)
    }
}
