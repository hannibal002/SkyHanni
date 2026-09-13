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
import at.hannibal2.skyhanni.utils.collection.CircularList
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import net.minecraft.world.entity.monster.Shulker

@SkyHanniModule
object ShulkerFinder {
    private val config get() = SkyHanniMod.feature.hunting

    private var storedRoute: CircularList<LorenzVec>? = null

    private var navigating = false

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onKeyPress(event: KeyPressEvent) {
        if (MinecraftCompat.screen != null || navigating) return
        if (event.keyCode != config.nextShulkerKeybind) return
        if (!config.shulkerFinder) return

        val shulkerType = ShulkerType.entries.firstOrNull { it.island.isInIsland() } ?: return
        navigateToNextShulker(shulkerType)
    }

    private fun navigateToNextShulker(shulkerType: ShulkerType) {
        // TODO add generic repo outdated error logic here
        val route = storedRoute ?: calculateRoute(shulkerType)
        storedRoute = route

        val goal = route.next()

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
    private fun onWorldChange() {
        navigating = false
        storedRoute = null
    }

    private fun calculateRoute(shulkerType: ShulkerType): CircularList<LorenzVec> {
        val graph = IslandGraphs.currentIslandGraph ?: error("Current island graph is null and there is a mistake")
        val list = graph.getNodesWithTags(shulkerType.nodeTag)

        return CircularList(NavigationUtils.getRouteLocations(list))
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
