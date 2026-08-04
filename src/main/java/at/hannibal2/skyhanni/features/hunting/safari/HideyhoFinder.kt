package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllHelper
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationCondition
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HideyhoFinder {

    private val config get() = SkyHanniMod.feature.hunting.safari

    private val patternGroup = RepoPattern.group("hunting.safari.hideyho-finder")

    private val startPattern by patternGroup.pattern(
        "start", "\\[MOB] Hideyho: Hehe, you found me!",
    )

    private val beginHidingPattern by patternGroup.pattern(
        "begin-hiding", "\\[MOB] Hideyho: No peeking!",
    )

    private val endPattern by patternGroup.pattern(
        "end", "\\[MOB] Hideyho: Aah! You found me!",
    )

    private val HIDEYHO_TEXTURE get() = SkullTextureHolder.getTexture("HIDEYHO")

    private var currentlyNavigating = true
    private var startLocation: LorenzVec? = null
    private var reportBug = false

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.hideyhoFinder) return

        if (startPattern.matches(event.cleanMessage)) {
            startLocation = LocationUtils.playerLocation().nearbyHideyhoLocation(10.0)
        }

        if (endPattern.matches(event.cleanMessage)) {
            if (!reportBug) return

            val playerLocation = LocationUtils.playerLocation()
            val nearbyHideyho = playerLocation.nearbyHideyhoLocation(10.0)

            IslandGraphs.reportLocation(
                LocationUtils.playerLocation(),
                userFacingReason = "unknown hideyho location",
                technicalInfo = "user found a hideyho while far from known hideyho locations",
                "nearbyHideyho" to nearbyHideyho,
            )

            reportBug = false
        }

        if (beginHidingPattern.matches(event.cleanMessage)) {
            // Wait for Hideyho to teleport first
            DelayedRun.runDelayed(2.seconds) {
                beginNavigation()
            }
        }
    }

    // TODO in future once NavigateAllHelper has the technology we can skip nodes that don't have a hideyho after doing a sight check
    private fun beginNavigation() {
        val startLocation = startLocation ?: return
        val graph = IslandGraphs.currentIslandGraph ?: return
        val locations = graph.getNodesWithTags(GraphNodeTag.HIDEYHO_LOCATION).toMutableList()
        val current = locations.minBy { it.position.distance(startLocation) }

        locations.remove(current)

        currentlyNavigating = true
        NavigateAllHelper.navigateAll(
            locations,
            GraphNodeTag.HIDEYHO_LOCATION.displayName,
            GraphNodeTag.HIDEYHO_LOCATION.color.toColor(),
            onFinish = {
                // If we finish going to all locations but do not find a Hideyho then we have a new location, so we get users to report
                reportBug = true
                currentlyNavigating = false
                NavigateAllHelper.handleStop()
            },
            continueNavigationCondition = NavigationCondition.SecondPassed { nodeLocation ->
                val isNearby = nodeLocation.position.nearbyHideyhoLocation(5.0) != null

                if (isNearby) {
                    ChatUtils.chat("§aFound Hideyho!")
                    currentlyNavigating = false
                    NavigateAllHelper.handleStop()
                }

                // If there is no Hideyho nearby then we go to the next location
                !isNearby
            },
            condition = { currentlyNavigating },
        )
    }

    private fun LorenzVec.nearbyHideyhoLocation(radius: Double): LorenzVec? {
        val nearbyEntities = this.getEntitiesNearby<RemotePlayer>(radius)
        return nearbyEntities.firstOrNull { it.getSkinTexture()?.equals(HIDEYHO_TEXTURE) ?: false }?.getLorenzVec()
    }

    @HandleEvent
    private fun onIslandLeave() {
        currentlyNavigating = false
        reportBug = false
    }

}
