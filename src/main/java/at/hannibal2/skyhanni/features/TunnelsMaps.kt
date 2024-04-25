package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraph
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.filterNotNullKeys
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class TunnelsMaps {

    var graph: Graph = Graph(emptyList())
    var goal: GraphNode? = null

    var possibleLocations = mapOf<String, List<GraphNode>>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        graph = event.getConstant<Graph>("TunnelsGraph", gson = Graph.gson)
        possibleLocations = graph.groupBy { it.name }.filterNotNullKeys()
    }

    fun getNearestNode() = graph.minBy { it.position.distanceSqToPlayer() }

    fun setGoalByName(name: String) = graph.firstOrNull { it.name == name }?.let {
        goal = it
    } ?: ErrorManager.logErrorStateWithData("Goal not found", "", "name" to name, "graph" to graph)

    @SubscribeEvent
    fun onIslandSwitch(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.DWARVEN_MINES) return/*         setGoalByName("§0Onyx Gemstone Mine") // TODO remove test */
    }

    val position = Position(20, 20)

    @SubscribeEvent
    fun onRenderDisplay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        val display = buildList<Renderable> {
            add(Renderable.string("§6Loactions:"))
            addAll(possibleLocations.map {
                Renderable.clickable(Renderable.string(it.key), onClick = {
                    goal = it.value.random()
                })
            })
        }
        position.renderRenderables(display, posLabel = "TunnelsMaps")
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        val goal = goal ?: return
        var path = graph.findShortestPathAsGraph(getNearestNode(), goal)
        val first = path.firstOrNull()
        val second = path.getOrNull(1)
        if (first != null && second != null) {
            val playerPosition = LocationUtils.playerLocation()
            val direct = playerPosition.distance(second.position)
            val around = playerPosition.distance(first.position) + first.neighbours[second]!!
            if (direct < around) {
                path = Graph(path.drop(1))
            }
        }
        path = Graph(listOf(GraphNode(-1, event.exactPlayerEyeLocation().add(-0.5, -0.5, -0.5))) + path)
        event.draw3DPathWithWaypoint(path, Color.GREEN, 7, true)
    }

}
