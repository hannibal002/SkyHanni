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
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import net.minecraft.world.entity.monster.Shulker

@SkyHanniModule
object ShulkerFinder {
    private val config get() = SkyHanniMod.feature.hunting

    private val route: MutableList<LorenzVec> = mutableListOf()
    private val storedRoute: MutableList<LorenzVec> = mutableListOf()

    private var navigating = false

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onKeyPress(event: KeyPressEvent) {
        if (MinecraftCompat.screen != null || navigating) return
        if (event.keyCode != config.nextShulkerKeybind) return
        if (!config.shulkerFinder) return

        val shulkerType = ShulkerType.entries.firstOrNull { it.island.isInIsland() } ?: return

        if (route.isEmpty()) {
            // TODO add generic repo outdated error logic here
            val newRoute = calculateRoute(shulkerType) ?: error("Current island graph is null and there is a mistake")
            route.addAll(newRoute)
            storedRoute.clear()
            storedRoute.addAll(newRoute)
        }

        navigateToNextShulker(shulkerType)
    }

    private fun navigateToNextShulker(shulkerType: ShulkerType) {
        val goal = route.removeFirstOrNull() ?: error("No shulker route found!")

        if (route.isEmpty()) {
            route.addAll(storedRoute)
        }

        navigating = true
        IslandGraphs.pathFind(
            goal,
            "nearest ${shulkerType.displayName}",
            shulkerType.color.toColor(),
            onFound = {
                val nearby = goal.getEntitiesNearby<Shulker>(3.0)
                if (nearby.isEmpty()) {
                    navigateToNextShulker(shulkerType)
                } else {
                    navigating = false
                }
            },
            condition = { config.shulkerFinder },
        )
    }

    @HandleEvent
    private fun onIslandChange() {
        navigating = false
        route.clear()
        storedRoute.clear()
    }

    private fun calculateRoute(shulkerType: ShulkerType): MutableList<LorenzVec>? {
        val graph = IslandGraphs.currentIslandGraph ?: return null
        val list = graph.getNodesWithTags(shulkerType.nodeTag)

        return NavigationUtils.getRouteLocations(list).toMutableList()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(140, "hunting.hideonleafFinder", "hunting.shulkerFinder")
        event.move(140, "hunting.nextHideonleafKeybind", "hunting.nextShulkerKeybind")
    }
}

private enum class ShulkerType(val displayName: String, val island: IslandType, val nodeTag: GraphNodeTag, val color: LorenzColor) {
    HIDEONLEAF("§2Hideonleaf", IslandType.GALATEA, GraphNodeTag.HIDEONLEAF, LorenzColor.DARK_GREEN),
    HIDEONSUN("§eHideonsun", IslandType.TORRHUS_CANYON, GraphNodeTag.HIDEONSUN, LorenzColor.YELLOW),
}
