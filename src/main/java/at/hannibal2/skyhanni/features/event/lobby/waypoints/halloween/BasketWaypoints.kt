package at.hannibal2.skyhanni.features.event.lobby.waypoints.halloween

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.jsonobjects.repo.EventWaypointsJson
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.event.lobby.waypoints.EventWaypoint
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object BasketWaypoints {

    private val config get() = SkyHanniMod.feature.event.lobbyWaypoints.halloweenBasket

    private val waypointList: MutableList<EventWaypoint> = mutableListOf()
    private var closest: EventWaypoint? = null
    private var isHalloween: Boolean = false

    private val foundBasketMessage by RepoPattern.pattern(
        "event.lobby.halloween.basket.found",
        "^(?:§.)+You(?: already)? found (?:a|this) Candy Basket!(?: (?:§.)+\\((?:§.)+(?<current>\\d+)(?:§.)+/(?:§.)+(?<max>\\d+)(?:§.)+\\))?\$"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.allWaypoints) return
        if (!isHalloween) return

        if (!isEnabled()) return

        if (foundBasketMessage.matches(event.message)) {
            val basket = waypointList.minByOrNull { it.position.distanceSqToPlayer() }!!
            basket.isFound = true
            if (closest == basket) {
                closest = null
            }
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.allWaypoints) return
        if (!isEnabled()) return

        isHalloween = checkScoreboardHalloweenSpecific()

        if (isHalloween && config.onlyClosest) {
            if (closest == null) {
                val notFoundBaskets = waypointList.filter { !it.isFound }
                if (notFoundBaskets.isEmpty()) return
                closest = notFoundBaskets.minByOrNull { it.position.distanceSqToPlayer() }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!isHalloween) return

        if (config.allWaypoints) {
            for (basket in waypointList) {
                if (!basket.shouldShow()) continue
                event.drawWaypointFilled(basket.position, LorenzColor.GOLD.toColor())
                event.drawDynamicText(basket.position, "§6Basket", 1.5)
            }
        }
    }

    private fun EventWaypoint.shouldShow(): Boolean {
        if (isFound) return false

        return if (config.onlyClosest) closest == this else true
    }

    // TODO use regex with the help of knowing the original lore. Will most likely need to wait until next halloween event
    private fun checkScoreboardHalloweenSpecific(): Boolean {
        val a = ScoreboardData.sidebarLinesFormatted.any { it.contains("Hypixel Level") }
        val b = ScoreboardData.sidebarLinesFormatted.any { it.contains("Halloween") }
        val c = ScoreboardData.sidebarLinesFormatted.any { it.contains("Baskets") }
        return a && b && c
    }

    private fun isEnabled() = HypixelData.hypixelLive && !LorenzUtils.inSkyBlock

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(13, "event.halloweenBasket", "event.lobbyWaypoints.halloweenBasket")
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        waypointList.clear()
        val basketList = event.getConstant<EventWaypointsJson>("EventWaypoints").baskets["lobby"] ?: emptyList()

        waypointList.addAll(basketList.map { EventWaypoint(it.position) })
    }
}
