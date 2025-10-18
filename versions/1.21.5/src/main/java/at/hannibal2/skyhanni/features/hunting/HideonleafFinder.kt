package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils

@SkyHanniModule
object HideonleafFinder {
    private val config get() = SkyHanniMod.feature.hunting

    private var route: MutableList<LorenzVec>? = null
    private var navigating = false

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onKeyPress(event: KeyPressEvent) {
        if (event.keyCode != config.nextHideonleafKeybind) return
        if (!config.hideonleafFinder) return
        if (navigating) return

        val route = route?.takeIfNotEmpty() ?: run {
            calculateRoute().also { route = it } ?:
            // TODO add generic repo outdated error logic here
            error("Current island graph is null and there is a mistake")
        }

        val goal = route.removeFirstOrNull() ?: error("No hideonleaf route found in galatea!")
        navigating = true
        IslandGraphs.pathFind(
            goal,
            "nearest §2Hideonleaf",
            LorenzColor.DARK_GREEN.toColor(),
            onFound = {
                // TODO auto start navigating to next if no hideonleaf nearby
                navigating = false
            },
            condition = { config.hideonleafFinder },
        )
    }

    @HandleEvent(IslandChangeEvent::class)
    fun onIslandChange() {
        navigating = false
        route = null
    }

    private fun calculateRoute(): MutableList<LorenzVec>? {
        val graph = IslandGraphs.currentIslandGraph ?: return null
        val list = graph.filter { it.hasTag(GraphNodeTag.HIDEONLEAF) }

        return NavigationUtils.getRoute(list, maxIterations = 300, neighborhoodSize = 50).toMutableList()
    }
}
