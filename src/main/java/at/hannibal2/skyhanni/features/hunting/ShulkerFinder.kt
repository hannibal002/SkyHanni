package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils

@SkyHanniModule
object ShulkerFinder {
    private val config get() = SkyHanniMod.feature.hunting

    private var route: MutableList<LorenzVec>? = null
    private var storedRoute: MutableList<LorenzVec>? = null

    private var navigating = false

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onKeyPress(event: KeyPressEvent) {
        if (MinecraftCompat.screen != null || navigating) return
        if (event.keyCode != config.nextShulkerKeybind) return
        if (!config.shulkerFinder) return

        val shulkerType = ShulkerType.entries.firstOrNull { it.island.isInIsland() } ?: return

        val currentRoute = route?.takeIfNotEmpty() ?: run {
            calculateRoute(shulkerType).also {
                route = it
                storedRoute = it
            } ?: error("Current island graph is null and there is a mistake")
            // TODO add generic repo outdated error logic here
        }

        val goal = currentRoute.removeFirstOrNull() ?: error("No shulker route found!")

        if (currentRoute.isEmpty()) {
            route = storedRoute
        }

        navigating = true
        IslandGraphs.pathFind(
            goal,
            "nearest ${shulkerType.displayName}",
            LorenzColor.DARK_GREEN.toColor(),
            onFound = {
                // TODO auto start navigating to next if no shulker nearby
                navigating = false
            },
            condition = { config.shulkerFinder },
        )
    }

    @HandleEvent
    private fun onIslandChange() {
        navigating = false
        route = null
    }

    private fun calculateRoute(shulkerType: ShulkerType): MutableList<LorenzVec>? {
        val graph = IslandGraphs.currentIslandGraph ?: return null
        val list = graph.filter { it.hasTag(shulkerType.nodeTag) }

        return NavigationUtils.getRoute(list, maxIterations = 300, neighborhoodSize = 50).toMutableList()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(140, "hunting.hideonleafFinder", "hunting.shulkerFinder")
        event.move(140, "hunting.nextHideonleafKeybind", "hunting.nextShulkerKeybind")
    }
}

private enum class ShulkerType(val displayName: String, val island: IslandType, val nodeTag: GraphNodeTag) {
    HIDEONLEAF("§2Hideonleaf", IslandType.GALATEA, GraphNodeTag.HIDEONLEAF),
    HIDEONSUN("§eHideonleaf", IslandType.TORRHUS_CANYON, GraphNodeTag.HIDEONSUN),
}
