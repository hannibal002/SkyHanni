package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findShortestDistance
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraph
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.filterNotNullKeys
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class TunnelsMaps {

    private val config get() = SkyHanniMod.feature.mining.tunnelMaps

    private var graph: Graph = Graph(emptyList())
    private lateinit var campfire: GraphNode
    private var goal: GraphNode? = null
    private var goalReached = false

    private var closedNote: GraphNode? = null
    private var path: Graph? = null

    private var possibleLocations = mapOf<String, List<GraphNode>>()
    private val cooldowns = mutableMapOf<GraphNode, SimpleTimeMark>()
    private var active: String = ""

    private fun getNext(name: String = active): GraphNode? {
        val closed = closedNote ?: return null
        val list = possibleLocations[name] ?: return null

        val offCooldown = list.filter { cooldowns[it]?.isInPast() != false }
        val goodOnes = offCooldown.filter { it.position.distanceSqToPlayer() > 400.0 }
        val best = goodOnes.minByOrNull { graph.findShortestDistance(closed, it) }
            ?: list.random()

        cooldowns[best] = 25.0.seconds.fromNow()
        goalReached = false
        return best
    }

    private fun hasNext(name: String = active): Boolean {
        val list = possibleLocations[name] ?: return false
        return list.size > 1
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        graph = event.getConstant<Graph>("TunnelsGraph", gson = Graph.gson)
        possibleLocations = graph.groupBy { it.name }.filterNotNullKeys().mapValues { (_, value) ->
            value
        }
        campfire = graph.first { it.name?.contains("Campfire") ?: false }
    }

    fun setGoalByName(name: String) = graph.firstOrNull { it.name == name }?.let {
        goal = it
    } ?: ErrorManager.logErrorStateWithData("Goal not found", "", "name" to name, "graph" to graph)

    @SubscribeEvent
    fun onRenderDisplay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val display = buildList<Renderable> {
            if (active.isNotEmpty()) {
                add(Renderable.string("§6Active: §f$active"))
                if (hasNext()) {
                    add(Renderable.clickable(Renderable.string("§eNext Spot"), onClick = {
                        goal = getNext()
                    }))
                }
            }
            add(Renderable.string("§6Loactions:"))
            val fairySouls = possibleLocations.filter { it.key.contains("Fairy") }
            val other = possibleLocations.filterNot { fairySouls.contains(it.key) }

            add(
                Renderable.hoverable(
                    Renderable.horizontalContainer(
                        listOf(Renderable.string("§dFairy Souls")) + fairySouls.map {
                            val name = it.key.removePrefix("§dFairy Soul ")
                            Renderable.clickable(Renderable.string("[${name}]"), onClick = {
                                active = it.key
                                goal = getNext()
                            })
                        }

                    ),
                    Renderable.string("§dFairy Souls")
                )
            )

            addAll(other.map {
                Renderable.clickable(Renderable.string(it.key), onClick = {
                    active = it.key
                    goal = getNext()
                })
            })
        }
        config.position.renderRenderables(display, posLabel = "TunnelsMaps")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        checkGoalReached()
        closedNote = graph.minBy { it.position.distanceSqToPlayer() }
        val closest = closedNote ?: return
        val goal = goal ?: return
        path = graph.findShortestPathAsGraph(closest, goal)
        val path = path ?: return
        val first = path.firstOrNull()
        val second = path.getOrNull(1)
        if (first != null && second != null) {
            val playerPosition = LocationUtils.playerLocation()
            val direct = playerPosition.distance(second.position)
            val around = playerPosition.distance(first.position) + first.neighbours[second]!!
            if (direct < around) {
                this.path = Graph(path.drop(1))
            }
        }

    }

    private fun checkGoalReached() {
        if (goalReached) return
        val distance = goal?.position?.distanceSqToPlayer() ?: return
        goalReached = distance < 25.0
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (goalReached) return
        val path = path ?: return
        event.draw3DPathWithWaypoint(path, Color.GREEN, 7, true)
    }

    @SubscribeEvent
    fun onKeyPress(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        if (event.keyCode != config.campfireKey) return
        if (config.travelScroll) {
            ChatUtils.sendMessageToServer("/warp basecamp")
        } else {
            goalReached = false
            goal = campfire
            active = campfire.name!!
        }
    }

    val areas = setOf(
        "Glacite Tunnels",
        "Dwarven Base Camp",
        "Glacite Lake",
        "Fossil Research Center"
    )

    private fun isEnabled() =
        IslandType.DWARVEN_MINES.isInIsland() && config.enable && areas.contains(LorenzUtils.skyBlockArea)
}
