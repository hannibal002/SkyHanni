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
        "first-found", "\\[MOB] Hideyho: Hehe, you found me!",
    )

    private val beginHidingPattern by patternGroup.pattern(
        "begin-hiding", "\\[MOB] Hideyho: No peeking!",
    )

    private val endPattern by patternGroup.pattern(
        "found-after-hiding", "\\[MOB] Hideyho: Aah! You found me!",
    )

    private val SKIN_TEXTURE by SkullTextureHolder.texture("HIDEYHO")

    private var currentlyNavigating = false
    private var startLocation: LorenzVec? = null
    private var reportBug = false

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.hideyhoFinder) return

        val playerLocation = LocationUtils.playerLocation()

        if (startPattern.matches(event.cleanMessage)) {
            startLocation = playerLocation.nearbyLocation(10.0)
            return
        }

        if (endPattern.matches(event.cleanMessage)) {
            if (!reportBug) return

            val playerLocation = playerLocation
            val nearbyHideyho = playerLocation.nearbyLocation(10.0)

            IslandGraphs.reportLocation(
                playerLocation,
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
        val current = locations.minByOrNull { it.position.distance(startLocation) }

        locations.remove(current)
        if (locations.isEmpty()) return

        currentlyNavigating = true
        NavigateAllHelper.navigateAll(
            locations,
            GraphNodeTag.HIDEYHO_LOCATION.displayName,
            GraphNodeTag.HIDEYHO_LOCATION.color.toColor(),
            onFinish = {
                // If we finish going to all locations but do not find a Hideyho then we have a new location, so we get users to report
                // Or maybe someone else already found one in your lobby
                // TODO is there a lootshare message we can use to end navigation early in this case
                ChatUtils.chat("Could not find any hidden Hideyho, maybe someone else already found it.")
                reportBug = true
                currentlyNavigating = false
                NavigateAllHelper.handleStop()
            },
            continueNavigationCondition = NavigationCondition.SecondPassed { nodeLocation ->
                val isNearby = nodeLocation.position.nearbyLocation(5.0) != null

                if (isNearby) {
                    finishNavigation()
                    return@SecondPassed false
                }

                // If there is no Hideyho nearby then we go to the next location
                return@SecondPassed true
            },
            condition = { config.hideyhoFinder && currentlyNavigating },
        )
    }

    private fun finishNavigation() {
        ChatUtils.chat("§aFound Hideyho!")
        currentlyNavigating = false
        NavigateAllHelper.handleStop()
    }

    private fun LorenzVec.nearbyLocation(radius: Double): LorenzVec? {
        val nearbyEntities = this.getEntitiesNearby<RemotePlayer>(radius)
        return nearbyEntities.firstOrNull { it.getSkinTexture() == SKIN_TEXTURE }?.getLorenzVec()
    }

    @HandleEvent
    private fun onIslandLeave() {
        currentlyNavigating = false
        reportBug = false
    }

}
