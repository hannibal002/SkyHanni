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
    lateinit var campfire: GraphNode
    var goal: GraphNode? = null

    var possibleLocations = mapOf<String, List<GraphNode>>()
    val locationIndex = mutableMapOf<String, Int>()
    var active: String = ""

    fun getNext(name: String): GraphNode? {
        val list = possibleLocations[name] ?: return null
        val preIndex = locationIndex[name]
        val index = when {
            preIndex == null -> 0
            preIndex >= list.lastIndex -> 0
            else -> preIndex + 1
        }
        locationIndex[name] = index
        return list[index]
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        graph = event.getConstant<Graph>("TunnelsGraph", gson = Graph.gson)
        possibleLocations = graph.groupBy { it.name }.filterNotNullKeys().mapValues { (_, value) ->
            val randomPick = value.random()
            value.sortedBy { it.position.distanceSq(randomPick.position) }
        }
        campfire = graph.first { it.name?.contains("Campfire") ?: false }
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
            if (active.isNotEmpty()) {
                add(Renderable.string("§6Active: $active"))
            }
            add(Renderable.string("§6Loactions:"))
            val fairySouls = possibleLocations.filter { it.key.contains("Fairy") }
            val other = possibleLocations.filterNot { it.key.contains("Fairy") }

            /* add(Renderable.hoverable(
                Renderable.string("§dFairy Souls"),
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.string("§dFairy Souls"),
                        Renderable.clickable(Renderable.string("[Lake]"), onClick = {
                            active = ""
                        })
                    )
                )
            )) */

            addAll(possibleLocations.map {
                Renderable.clickable(Renderable.string(it.key), onClick = {
                    active = it.key
                    goal = getNext(it.key)
                })
            })
            add(Renderable.clickable(Renderable.string("Next"), onClick = {
                goal = getNext(active)
            }))
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
