package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraphWithDistance
import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.io.File

/**
 * TODO
 *
 * benefits of every island graphs:
 * global:
 * 	hoppity eggs rabbit
 * 	NEU's fairy souls
 * 	point of interests/areas
 * 	slayer area
 * 	NEU's NPC's
 * 	races (end, park, winter, dungeon hub)
 * 	jump pads between servers
 * 	ring of love/romeo juliet quest
 * 	death location
 * hub:
 * 	12 starter NPC's
 * 	diana
 * farming:
 * 	pelt farming
 * rift:
 * 	enigma souls
 * 	eyes
 * 	big quests
 * spider:
 * 	relicts + throw spot
 * dwarven mines:
 * 	emissary
 * 	commssion areas
 * 	events: raffle, goblin slayer, donpieresso
 * deep
 * 	path to the bottom
 * end
 * 	golem spawn
 * 	dragon death spot
 * crimson
 *  vanquisher path
 *  area mini bosses
 *  daily quests
 *  intro tutorials with elle
 *
 * graph todo:
 * 	create category for nodes
 * 	better detection of cloesest node
 * 	fix rename not using tick but input event we have (+ create the input event in the first place)
 * 	toggle distance to node by node path lengh, instead of eye of sight lenght
 * 	press test button again to enable "true test mode", with graph math and hiding other stuff
 * 	option to compare two graphs, and store multiple graphs in the edit mode in paralell
 * 	add support for /shtestwaypoint
 *
 * done
 * 	create selection block, instead of using the player location in the other node features
 *
 *
 * area features:
 * 	show list of only areas, most cloesest to you
 * 	also show entry/exit signs while passing through
 */

@SkyHanniModule
object IslandGraphs {
    var currentIslandGraph: Graph? = null

    val existsForThisIsland get() = currentIslandGraph != null

    var closedNote: GraphNode? = null

    private var currentTarget: LorenzVec? = null
    private var color = Color.WHITE
    private var showGoalExact = false
    private var onFound: () -> Unit = {}
    private var goal: GraphNode? = null
        set(value) {
            prevGoal = field
            field = value
        }
    private var prevGoal: GraphNode? = null

    private var path: Pair<Graph, Double>? = null

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        if (!LorenzUtils.inSkyBlock) return

        reloadFromJson(LorenzUtils.skyBlockIsland)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        reloadFromJson(event.newIsland)
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        reset()
    }

    private fun reloadFromJson(newIsland: IslandType) {
        val islandName = newIsland.name
        val constant = "island_graphs/$islandName"
        val name = "constants/$constant.json"
        val jsonFile = File(SkyHanniMod.repo.repoLocation, name)
        if (!jsonFile.isFile) {
            currentIslandGraph = null
            return
        }

        val graph = RepoUtils.getConstant(SkyHanniMod.repo.repoLocation, constant, Graph.gson, Graph::class.java)
        setNewGraph(graph)
    }

    fun setNewGraph(graph: Graph) {
        reset()
        currentIslandGraph = graph
    }

    private fun reset() {
        closedNote = null
        currentTarget = null
        goal = null
        path = null
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val prevClosed = closedNote

        val graph = currentIslandGraph ?: return

        currentTarget?.let {
            if (it.distanceToPlayer() < 3) {
                onFound()
                reset()
                return
            }
        }

        val newClosest = graph.minBy { it.position.distanceSqToPlayer() }
        if (closedNote == newClosest) return
        closedNote = newClosest
        onNewNote()
        val closest = closedNote ?: return
        val goal = goal ?: return
        if (closest == prevClosed) return

        val (path, distance) = graph.findShortestPathAsGraphWithDistance(closest, goal)
        val first = path.firstOrNull()
        val second = path.getOrNull(1)

        val playerPosition = LocationUtils.playerLocation()
        val nodeDistance = first?.let { playerPosition.distance(it.position) } ?: 0.0
        if (first != null && second != null) {
            val direct = playerPosition.distance(second.position)
            val firstPath = first.neighbours[second] ?: 0.0
            val around = nodeDistance + firstPath
            if (direct < around) {
                this.path = Graph(path.drop(1)) to (distance - firstPath + direct)
                return
            }
        }
        this.path = path to (distance + nodeDistance)
    }

    private fun onNewNote() {
        // TODO create an event
        IslandAreas.noteMoved()
    }

    fun stop() {
        currentTarget = null
        goal = null
        path = null
    }

    fun find(location: LorenzVec, color: Color = LorenzColor.WHITE.toColor(), onFound: () -> Unit = {}, showGoalExact: Boolean = false) {
        reset()
        currentTarget = location
        this.color = color
        this.onFound = onFound
        val graph = currentIslandGraph ?: return
        goal = graph.minBy { it.position.distance(currentTarget!!) }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val path = path?.takeIf { it.first.isNotEmpty() } ?: return
        val graph = path.first
        event.draw3DPathWithWaypoint(
            graph,
            color,
            6,
            true,
            bezierPoint = 2.0,
            textSize = 1.0,
        )
        val lastNode = graph.graph.last().position
        val targetLocation = currentTarget ?: return
        event.draw3DLine(lastNode.add(0.5, 0.5, 0.5), targetLocation.add(0.5, 0.5, 0.5), color, 4, true)

        if (showGoalExact) {
            event.drawWaypointFilled(targetLocation, color)
        }
    }
}
