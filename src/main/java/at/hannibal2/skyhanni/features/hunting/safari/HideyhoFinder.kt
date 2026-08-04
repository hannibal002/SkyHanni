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
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer

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

    val HIDEYHO_TEXTURE get() = SkullTextureHolder.getTexture("HIDEYHO")

    private var currentlyNavigating = true
    private var startLocation: LorenzVec? = null
    private var reportBug = false

    @HandleEvent(onlyOnIsland = IslandType.SAFARI)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.hideyhoFinder) return

        if (startPattern.matches(event.cleanMessage)) {
            startLocation = LocationUtils.playerLocation().nearbyHideyhoLocation()
        }

        if (endPattern.matches(event.cleanMessage)) {
            if (!reportBug) return

            val playerLocation = LocationUtils.playerLocation()

            val nearbyHideyho = playerLocation.nearbyHideyhoLocation()

            IslandGraphs.reportLocation(
                LocationUtils.playerLocation(),
                userFacingReason = "unknown hideyho location",
                technicalInfo = "user found a hideyho while far from known hideyho locations",
                "nearbyHideyho" to nearbyHideyho,
            )

            reportBug = false
            return
        }
        if (beginHidingPattern.matches(event.cleanMessage)) {
            val startLocation = startLocation ?: return
            val nearbyEntities = startLocation.getEntitiesNearby<RemotePlayer>(10.0)

            val hideyhoLocation = nearbyEntities.firstOrNull { it.getSkinTexture()?.equals(HIDEYHO_TEXTURE) ?: false }

            hideyhoLocation ?: return

            val graph = IslandGraphs.currentIslandGraph ?: return
            val locations = graph.getNodesWithTags(GraphNodeTag.HIDEYHO_LOCATION).toMutableList()
            val current = locations.minBy { it.position.distance(hideyhoLocation.getLorenzVec()) }
            locations.remove(current)

            currentlyNavigating = true
            NavigateAllHelper.navigateAll(
                locations,
                GraphNodeTag.HIDEYHO_LOCATION.displayName,
                GraphNodeTag.HIDEYHO_LOCATION.color.toColor(),
                onFinish = {
                    // If we finish going to all locations but do not find a hideyho then we have a new location
                    currentlyNavigating = false
                    reportBug = true
                    NavigateAllHelper.handleStop()
                },
                continueNavigationCondition = NavigationCondition.SecondPassed { nodeLocation ->
                    val isNearby = nodeLocation.position.getEntitiesNearby<RemotePlayer>(5.0)
                        .any { it.getSkinTexture()?.equals(HIDEYHO_TEXTURE) ?: false }

                    if (isNearby) {
                        ChatUtils.chat("§aFound Hideyho!")
                        currentlyNavigating = false
                        NavigateAllHelper.handleStop()
                    }

                    !isNearby
                },
                condition = { currentlyNavigating },
            )
        }
    }

    private fun LorenzVec.nearbyHideyhoLocation(): LorenzVec? {
        val nearbyEntities = this.getEntitiesNearby<RemotePlayer>(10.0)
        return nearbyEntities.firstOrNull { it.getSkinTexture()?.equals(HIDEYHO_TEXTURE) ?: false }?.getLorenzVec()
    }

    @HandleEvent
    private fun onIslandLeave() {
        currentlyNavigating = false
        reportBug = false
    }

}
