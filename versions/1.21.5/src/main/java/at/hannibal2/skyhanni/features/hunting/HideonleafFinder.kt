package at.hannibal2.hanni.features.hunting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.GraphNodeTag
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.minecraft.KeyPressEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.hanni.utils.navigation.NavigationUtils

@HanniModule
object HideonleafFinder {
    private val config get() = HanniMod.feature.hunting

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
