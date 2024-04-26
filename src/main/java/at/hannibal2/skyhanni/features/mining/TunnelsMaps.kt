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
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.filterNotNullKeys
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
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

    private var goalReached = false
    private var prevGoal: GraphNode? = null
    private var goal: GraphNode? = null
        set(value) {
            prevGoal = field
            field = value
        }

    private var closedNote: GraphNode? = null
    private var path: Graph? = null

    private var possibleLocations = mapOf<String, List<GraphNode>>()
    private val cooldowns = mutableMapOf<GraphNode, SimpleTimeMark>()
    private var active: String = ""

    private lateinit var fairySouls: Map<String, GraphNode>
    private lateinit var normalLocations: Map<String, List<GraphNode>>

    private fun getNext(name: String = active): GraphNode? {
        fairySouls[name]?.let {
            goalReached = false
            return it
        }

        val closed = closedNote ?: return null
        val list = possibleLocations[name] ?: return null

        val offCooldown = list.filter { cooldowns[it]?.isInPast() != false }
        val goodOnes = offCooldown.filter { it.position.distanceSqToPlayer() > 400.0 }
        val best = goodOnes.minByOrNull { graph.findShortestDistance(closed, it) }
            ?: list.minBy { cooldowns[it] ?: SimpleTimeMark.farPast() }

        cooldowns[best] = 25.0.seconds.fromNow()
        goalReached = false
        return best
    }

    private fun hasNext(name: String = active): Boolean {
        val list = possibleLocations[name] ?: return false
        return list.size > 1
    }

    @SubscribeEvent // TODO lift up all nodes from the ground
    fun onRepoReload(event: RepositoryReloadEvent) {
        graph = event.getConstant<Graph>("TunnelsGraph", gson = Graph.gson)
        possibleLocations = graph.groupBy { it.name }.filterNotNullKeys().mapValues { (_, value) ->
            value
        }
        campfire = graph.first { it.name?.contains("Campfire") ?: false }
        fairySouls = possibleLocations.filter { it.key.contains("Fairy") }.mapValues { it.value.first() }
        normalLocations = possibleLocations.filterNot { fairySouls.contains(it.key) && campfire.name != it.key }
    }

    @SubscribeEvent
    fun onRenderDisplay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val display = buildList<Renderable> {
            if (active.isNotEmpty()) {
                if (goal == campfire && active != campfire.name) {
                    add(Renderable.string("§6Override for ${campfire.name}"))
                    add(Renderable.clickable(Renderable.string("§eMake §f$active §eactive"), onClick = {
                        goal = getNext()
                    }))
                } else {
                    add(Renderable.string("§6Active: §f$active"))
                    if (hasNext()) {
                        add(Renderable.clickable(Renderable.string("§eNext Spot"), onClick = {
                            goal = getNext()
                        }))
                    } else {
                        add(Renderable.string(""))
                    }
                }
            } else {
                add(Renderable.string(""))
                add(Renderable.string(""))
            }
            add(Renderable.string("§6Loactions:"))
            add(
                Renderable.multiClickAndHover(
                    campfire.name!!,
                    listOf(
                        "§eLeft Click to set active",
                        "§eRight Click for override"
                    ),
                    click = mapOf(
                        0 to guiSetActive(campfire.name!!),
                        1 to ::campfireOverride
                    )
                )
            )
            add(
                Renderable.hoverable(
                    Renderable.horizontalContainer(
                        listOf(Renderable.string("§dFairy Souls")) + fairySouls.map {
                            val name = it.key.removePrefix("§dFairy Soul ")
                            Renderable.clickable(Renderable.string("§d[${name}]"), onClick = guiSetActive(it.key))
                        }

                    ),
                    Renderable.string("§dFairy Souls")
                )
            )

            addAll(normalLocations.map {
                Renderable.clickable(Renderable.string(it.key), onClick = guiSetActive(it.key))
            })
        }
        config.position.renderRenderables(display, posLabel = "TunnelsMaps")
    }

    private fun campfireOverride() {
        goalReached = false
        goal = campfire
    }

    private fun guiSetActive(it: String): () -> Unit = {
        active = it
        goal = getNext()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (checkGoalReached()) return
        val prevClosed = closedNote
        closedNote = graph.minBy { it.position.distanceSqToPlayer() }
        val closest = closedNote ?: return
        val goal = goal ?: return
        if (closest == prevClosed && goal == prevGoal) return
        val path = graph.findShortestPathAsGraph(closest, goal)
        val first = path.firstOrNull()
        val second = path.getOrNull(1)
        if (first != null && second != null) {
            val playerPosition = LocationUtils.playerLocation()
            val direct = playerPosition.distance(second.position)
            val around = playerPosition.distance(first.position) + first.neighbours[second]!!
            if (direct < around) {
                this.path = Graph(path.drop(1))
                return
            }
        }
        this.path = path
    }

    private fun checkGoalReached(): Boolean {
        if (goalReached) return true
        val distance = goal?.position?.distanceSqToPlayer() ?: return false
        goalReached = distance < 36.0
        if (goalReached) {
            path = null
            return true
        }
        return false
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        val path = path ?: return
        event.draw3DPathWithWaypoint(path, getPathColor(), 7, true, bezierPoint = 2.0)
    }

    private fun getPathColor(): Color = if (config.dynamicPathColour) {
        goal?.name?.getFirstColorCode()?.toLorenzColor()?.takeIf { it != LorenzColor.WHITE }?.toColor()
    } else {
        null
    } ?: config.pathColour.toChromaColor()

    @SubscribeEvent
    fun onKeyPress(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        campfireKey(event)
        nextSpotKey(event)
    }

    private fun campfireKey(event: LorenzKeyPressEvent) {
        if (event.keyCode != config.campfireKey) return
        if (config.travelScroll) {
            ChatUtils.sendMessageToServer("/warp basecamp")
        } else {
            campfireOverride()
        }
    }

    private var nextSpotDelay = SimpleTimeMark.farPast()

    private fun nextSpotKey(event: LorenzKeyPressEvent) {
        if (event.keyCode != config.nextSpotHotkey) return
        if (!nextSpotDelay.isInPast()) return
        nextSpotDelay = 1.0.seconds.fromNow()
        goal = getNext()
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
