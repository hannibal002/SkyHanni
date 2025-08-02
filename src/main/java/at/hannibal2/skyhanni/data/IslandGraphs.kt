package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationFeedback
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DPathWithWaypoint
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityPlayerSP
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds

/**
 * TODO
 * benefits of every island graphs:
 * global:
 * 	NEU's fairy souls
 * 	slayer area (not all there yet)
 * 	NEU's NPC's (auto acitvate when searching via neu)
 * 	races (end, park, winter, dungeon hub)
 * 	jump pads between servers
 * 	ring of love/romeo juliet quest
 * 	death location
 * 	% of island discvovered (every node was most closest node at least once)
 * hub:
 * 	12 starter NPC's
 * 	diana
 * farming:
 * 	pelt farming area
 * rift:
 * 	eyes
 * 	big quests
 * 	montezuma souls
 * 	blood effigies
 * 	avoid area around enderman
 * spider:
 * 	relicts + throw spot
 * dwarven mines:
 * 	emissary
 * 	commssion areas
 * 	events: raffle, goblin slayer, donpieresso
 * deep
 * 	path to the bottom (Rhys NPC)
 * end
 * 	golem spawn
 * 	dragon death spot
 * crimson
 *  vanquisher path
 *  area mini bosses
 *  daily quests
 *  intro tutorials with elle
 *  fishing spots
 * mineshaft
 *  different types mapped out
 *  paths to ladder and possible corpse locations, and known corpse locations
 *
 * Additional global things:
 *  use custom graphs for your island/garden
 *  suggest using warp points if closer
 *  support cross island paths (have a list of all node names in all islands)
 *
 * Changes in graph editor:
 * 	fix rename not using tick but input event we have (+ create the input event in the first place)
 * 	toggle distance to node by node path lengh, instead of eye of sight lenght
 * 	press test button again to enable "true test mode", with graph math and hiding other stuff
 * 	option to compare two graphs, and store multiple graphs in the edit mode in paralell
 */

@SkyHanniModule
object IslandGraphs {

    var currentIslandGraph: Graph? = null
        private set
    private var lastLoadedIslandType = "nothing"
    private var lastLoadedTime = SimpleTimeMark.farPast()

    var disabledNodesReason: String? = null
        private set

    fun disableNodes(reason: String, center: LorenzVec, radius: Double) {
        val graph = currentIslandGraph ?: return
        disabledNodesReason = reason
        for (node in graph.nodes.filter { it.position.distance(center) < radius }) {
            node.enabled = false
        }
    }

    fun enableAllNodes() {
        disabledNodesReason = null
        val graph = currentIslandGraph ?: return
        graph.nodes.forEach { it.enabled = true }
    }

    private var pathfindClosestNode: GraphNode? = null
    var closestNode: GraphNode? = null

    private var currentTarget: LorenzVec? = null
    private var currentTargetNode: GraphNode? = null
    private var label = ""
    private var lastDistance = 0.0
    private var totalDistance = 0.0
    private var color = Color.WHITE
    private var shouldAllowRerouting = false
    private var onFound: () -> Unit = {}
    private var onManualCancel: () -> Unit = {}
    private var goal: GraphNode? = null
        set(value) {
            prevGoal = field
            field = value
        }
    private var prevGoal: GraphNode? = null

    private var fastestPath: Graph? = null
    private var condition: () -> Boolean = { true }
    private var inGlaciteTunnels: Boolean? = null

    private val patternGroup = RepoPattern.group("data.island.navigation")

    /**
     * REGEX-TEST: Dwarven Base Camp
     * REGEX-FAIL: Forge
     * REGEX-TEST: Fossil Research Center
     */
    private val glaciteTunnelsPattern by patternGroup.pattern(
        "glacitetunnels",
        "Glacite Tunnels|Dwarven Base Camp|Great Glacite Lake|Fossil Research Center",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onRepoReload(event: RepositoryReloadEvent) {
        loadIsland(SkyBlockUtils.currentIsland)
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        enableAllNodes()
        if (currentIslandGraph != null) return
        if (event.newIsland == IslandType.NONE) return
        loadIsland(event.newIsland)
    }

    @HandleEvent
    fun onWorldChange() {
        currentIslandGraph = null
        if (currentTarget != null) NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation stopped because of world switch!")
        reset()
    }

    private fun isGlaciteTunnelsArea(area: String?): Boolean = glaciteTunnelsPattern.matches(area)

    @HandleEvent
    fun onAreaChange(event: ScoreboardAreaChangeEvent) {
        if (!IslandType.DWARVEN_MINES.isCurrent()) {
            inGlaciteTunnels = null
            return
        }

        // can not use IslandAreas for area detection here. It HAS TO be the scoreboard
        val now = isGlaciteTunnelsArea(SkyBlockUtils.scoreboardArea)
        if (inGlaciteTunnels != now) {
            inGlaciteTunnels = now
            loadDwarvenMines()
        }
    }

    fun loadLobby(lobby: String) {
        reloadFromJson(lobby)
    }

    private fun loadDwarvenMines() {
        // can not use IslandAreas for area detection here. It HAS TO be the scoreboard
        if (isGlaciteTunnelsArea(SkyBlockUtils.scoreboardArea)) {
            reloadFromJson("GLACITE_TUNNELS")
        } else {
            reloadFromJson("DWARVEN_MINES")
        }
    }

    private fun loadIsland(newIsland: IslandType) {
        if (newIsland == IslandType.DWARVEN_MINES) {
            loadDwarvenMines()
        } else {
            reloadFromJson(newIsland.name)
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Graphs")
        val islandType = SkyBlockUtils.currentIsland.name
        val important = SkyBlockUtils.inSkyBlock && lastLoadedIslandType != islandType
        val list = buildList {
            add("")
            if (important) {
                add("wrong island!")
            } else {
                add("island is correct!")
            }
            add("")
            add("lastLoadedIslandType: $lastLoadedIslandType")
            if (important) {
                add("current islandType: $islandType")
            }

            add("")
            add("lastLoadedTime: ${lastLoadedTime.passedSince()}")
            if (important) {
                add("last world switch: ${SkyBlockUtils.lastWorldSwitch.passedSince()}")
            }

            add("")
            add("currentIslandGraph is null: ${currentIslandGraph == null}")
        }
        if (important) {
            event.addData(list)
        } else {
            event.addIrrelevant(list)
        }
    }

    private fun reloadFromJson(islandName: String) {
        lastLoadedIslandType = islandName
        lastLoadedTime = SimpleTimeMark.now()

        try {
            val graph = SkyHanniRepoManager.getRepoData<Graph>("constants/island_graphs", islandName, gson = Graph.gson)
            IslandAreas.display = null
            setNewGraph(graph)
        } catch (e: Error) {
            currentIslandGraph = null
            return
        }
    }

    fun setNewGraph(graph: Graph) {
        currentIslandGraph = graph
        if (currentTarget != null) {
            DelayedRun.runDelayed(500.milliseconds) {
                handleTick()
                checkMoved()
            }
        }

        // calling various update functions to make switching between deep caverns and glacite tunnels bearable
        handleTick()
        IslandGraphReloadEvent(graph).post()
    }

    private fun reset() {
        stop()
        pathfindClosestNode = null
        closestNode = null
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (currentIslandGraph == null) return
        if (event.isMod(2)) {
            update()
        }
        updateFeedback()
    }

    fun update(force: Boolean = false) {
        if (force) {
            pathfindClosestNode = null
        }
        handleTick()
        checkMoved()
    }

    private fun handleTick() {
        val prevClosest = pathfindClosestNode

        currentTarget?.let {
            if (it.distanceToPlayer() < 3) {
                NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation reached §r$label§e!")
                reset()
                onFound()
            }
            if (!condition()) {
                reset()
            }
        }

        val graph = currentIslandGraph ?: return
        val newClosest = graph.minBy { it.position.distanceSqToPlayer() }
        if (pathfindClosestNode == newClosest) return
        val newPath = !onCurrentPath()

        closestNode = newClosest
        onNewNode()
        if (newClosest == prevClosest) return
        if (newPath) {
            pathfindClosestNode = closestNode
            findNewPath()
        }
    }

    private fun onCurrentPath(): Boolean {
        val path = fastestPath ?: return false
        if (path.isEmpty()) return false
        val closest = path.nodes.minBy { it.position.distanceSqToPlayer() }
        val distance = closest.position.distanceToPlayer()
        if (distance > 7) return false

        val index = path.nodes.indexOf(closest)
        val newNodes = path.drop(index)
        val newGraph = Graph(newNodes)
        fastestPath = skipIfCloser(newGraph)
        setFastestPath(newGraph to newGraph.totalLength(), setPath = false)
        return true
    }

    private fun skipIfCloser(graph: Graph): Graph = if (graph.nodes.size > 1) {
        val hideNearby = if (MinecraftCompat.localPlayer.onGround) 3 else 5
        Graph(graph.nodes.takeLastWhile { it.position.distanceToPlayer() > hideNearby })
    } else {
        graph
    }

    private fun findNewPath() {
        val goal = goal ?: return
        val closest = pathfindClosestNode ?: return

        val (path, distance) = GraphUtils.findShortestPathAsGraphWithDistance(closest, goal)
        val first = path.firstOrNull()
        val second = path.getOrNull(1)

        val playerPosition = LocationUtils.playerLocation()
        val nodeDistance = first?.let { playerPosition.distance(it.position) } ?: 0.0
        if (first != null && second != null) {
            val direct = playerPosition.distance(second.position)
            val firstPath = first.neighbours[second] ?: 0.0
            val around = nodeDistance + firstPath
            if (direct < around) {
                setFastestPath(Graph(path.drop(1)) to (distance - firstPath + direct))
                return
            }
        }
        setFastestPath(path to (distance + nodeDistance))
    }

    private fun Graph.totalLength(): Double = nodes.zipWithNext().sumOf { (a, b) -> a.position.distance(b.position) }

    private fun handlePositionChange() {
        updateFeedback()
    }

    private var hasMoved = false

    private fun checkMoved() {
        if (hasMoved) {
            hasMoved = false
            if (goal != null) {
                handlePositionChange()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<EntityPlayerSP>) {
        if (currentIslandGraph != null && event.isLocalPlayer) {
            hasMoved = true
        }
    }

    private fun setFastestPath(path: Pair<Graph, Double>, setPath: Boolean = true) {
        // TODO cleanup
        val (fastestPath, _) = path.takeIf { it.first.isNotEmpty() } ?: return
        val nodes = fastestPath.nodes.toMutableList()
        if (MinecraftCompat.localPlayer.onGround) {
            nodes.add(0, GraphNode(0, LocationUtils.playerLocation()))
        }
        renderPath(setPath, nodes)
    }

    private fun renderPath(
        setPath: Boolean = true,
        nodes: List<GraphNode>,
    ) {
        if (setPath) {
            this.fastestPath = skipIfCloser(Graph(cutByMaxDistance(nodes, 2.0)))
        }
        updateFeedback()
    }

    private fun onNewNode() {
        // TODO create an event
        IslandAreas.nodeMoved()
        if (shouldAllowRerouting) {
            tryRerouting()
        }
    }

    private fun tryRerouting() {
        val target = currentTargetNode ?: return
        val closest = pathfindClosestNode ?: return
        val map = GraphUtils.findAllShortestDistances(closest).distances.filter { it.key.sameNameAndTags(target) }
        val newTarget = map.sorted().keys.firstOrNull() ?: return
        if (newTarget != target) {
            ChatUtils.debug("Rerouting navigation..")
            newTarget.pathFind(label, color, onFound, allowRerouting = true, condition = condition)
        }
    }

    fun stop() {
        currentTarget = null
        goal = null
        fastestPath = null
        currentTargetNode = null
        label = ""
        totalDistance = 0.0
        lastDistance = 0.0
        NavigationFeedback.setNavInactive()
    }

    /**
     * Activates pathfinding, with this graph node as goal.
     *
     * @param label The name of the naviation goal in chat.
     * @param color The color of the lines in world.
     * @param onFound The callback that gets fired when the goal is reached.
     * @param allowRerouting When a different node with same name and tags as the origianl goal is closer to the player, starts routing to this instead.
     * @param condition The pathfinding stops when the condition is no longer valid.
     */
    fun GraphNode.pathFind(
        label: String,
        color: Color = LorenzColor.WHITE.toColor(),
        onFound: () -> Unit = {},
        allowRerouting: Boolean = false,
        onManualCancel: () -> Unit = {},
        condition: () -> Boolean,
    ) {
        if (isActive(position, label)) return
        reset()
        currentTargetNode = this
        shouldAllowRerouting = allowRerouting
        pathFind0(location = position, label, color, onFound, onManualCancel, condition)
    }

    /**
     * Activates pathfinding to a location in the island.
     *
     * @param location The goal of the pathfinder.
     * @param label The name of the navigation goal in chat. Cannot be empty.
     * @param color The color of the lines in the world.
     * @param onFound The callback that gets fired when the goal is reached.
     * @param condition The pathfinding stops when the condition is no longer valid.
     */
    fun pathFind(
        location: LorenzVec,
        label: String,
        color: Color = LorenzColor.WHITE.toColor(),
        onFound: () -> Unit = {},
        onManualCancel: () -> Unit = {},
        condition: () -> Boolean,
    ) {
        if (isActive(location, label)) return
        require(label.isNotEmpty()) { "Label cannot be empty." }
        reset()
        shouldAllowRerouting = false
        pathFind0(location, label, color, onFound, onManualCancel, condition)
    }

    private fun pathFind0(
        location: LorenzVec,
        label: String,
        color: Color = LorenzColor.WHITE.toColor(),
        onFound: () -> Unit = {},
        onManualCancel: () -> Unit = {},
        condition: () -> Boolean,
    ) {
        currentTarget = location
        this.label = label
        this.color = color
        this.onFound = onFound
        this.onManualCancel = onManualCancel
        this.condition = condition
        val graph = currentIslandGraph ?: return
        goal = graph.minBy { it.position.distance(currentTarget!!) }
        updateFeedback()
    }

    private fun updateFeedback() {
        if (label == "") return
        val path = fastestPath ?: return
        var distance = 0.0
        if (path.isNotEmpty()) {
            for ((a, b) in path.zipWithNext()) {
                distance += a.position.distance(b.position)
            }
            val distanceToPlayer = path.first().position.distanceToPlayer()
            distance += distanceToPlayer
            distance = distance.roundTo(1)
        }

        if (distance == lastDistance) return
        lastDistance = distance
        if (distance == 0.0) return
        if (totalDistance == 0.0 || distance > totalDistance) {
            totalDistance = distance
        }

        val percentage = (1 - (distance / totalDistance)) * 100
        val component = "§e[SkyHanni] Navigating to §r$label §f[§e$distance§f] §f(§c${percentage.roundTo(1)}%§f)".asComponent()
        component.onClick(onClick = ::cancelClick)
        component.hover = "§eClick to stop navigating!".asComponent()
        NavigationFeedback.sendPathFindMessage(component)
    }

    fun cancelClick() {
        NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation stopped!")
        stop()
        onManualCancel()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (currentIslandGraph == null) return
        val path = fastestPath ?: return

        // maybe reuse for debuggin
//         for ((a, b) in path.nodes.zipWithNext()) {
//             val diff = a.position.distance(b.position)
//             event.drawString(a.position, "diff: ${diff.roundTo(1)}")
//         }
        event.draw3DPathWithWaypoint(
            path,
            color,
            6,
            true,
            bezierPoint = 0.6,
            textSize = 1.0,
        )

        val targetLocation = currentTarget ?: return
        val lastNode = path.nodes.lastOrNull()?.position ?: return
        event.draw3DLine(lastNode.add(0.5, 0.5, 0.5), targetLocation.add(0.5, 0.5, 0.5), color, 4, true)
    }

    // TODO move into new utils class
    private fun cutByMaxDistance(nodes: List<GraphNode>, maxDistance: Double): List<GraphNode> {
        var index = nodes.size * 10
        val locations = mutableListOf<LorenzVec>()
        var first = true
        for (node in nodes) {
            if (first) {
                first = false
            } else {
                var lastPosition = locations.last()
                val currentPosition = node.position
                val vector = (currentPosition - lastPosition).normalize()
                var distance = lastPosition.distance(currentPosition)
                while (distance > maxDistance) {
                    distance -= maxDistance
                    val nextStepDistance = if (distance < maxDistance / 2) {
                        (maxDistance + distance) / 2
                        break
                    } else maxDistance
                    val newPosition = lastPosition + (vector * (nextStepDistance))
                    locations.add(newPosition)
                    lastPosition = newPosition
                }
            }
            locations.add(node.position)
        }

        return locations.map { GraphNode(index++, it) }
    }

    fun isActive(testTarget: LorenzVec, testLabel: String): Boolean = testTarget == currentTarget && testLabel == label

    fun findClosestNode(location: LorenzVec, condition: (GraphNode) -> Boolean, radius: Double = 100.0): GraphNode? {
        val graph = currentIslandGraph ?: return null

        val found = graph.nodes.filter { condition(it) }.minBy { it.position.distanceSq(location) }
        return found.takeIf { it.position.distance(location) < radius }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shreportlocation") {
            description = "Allows the user to report an error with pathfinding at the current location."
            category = CommandCategory.USERS_BUG_FIX
            callback { reportCommand(it) }
        }
        event.register("shstopnavigation") {
            description = "Stops the current pathfinding."
            category = CommandCategory.USERS_ACTIVE
            callback {
                if (currentTarget != null) {
                    stop()
                    NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation stopped!")
                } else {
                    ChatUtils.userError("No navigation is currently active.")
                }
            }
        }
    }

    private fun reportCommand(args: Array<String>) {
        if (args.isEmpty()) {
            ChatUtils.userError("Usage: /shreportlocation <reason>")
            ChatUtils.chat(
                "Give a reason that explains what's wrong at this location, e.g.: " +
                    "pathfinding goes through wall, ignores obvious shortcut, " +
                    "missing npc/fishing hotspot/skyblock area name in /shnavigate..",
            )
            return
        }

        sendReportLocation(
            LocationUtils.playerLocation(),
            reasonForReport = "Manual reported graph location error",
            userReason = args.joinToString(" "),
            ignoreCache = true,
            betaOnly = false,
        )
    }

    fun reportLocation(
        location: LorenzVec,
        userFacingReason: String,
        additionalInternalInfo: String? = null,
        ignoreCache: Boolean = false,
        betaOnly: Boolean = false,
    ) {
        sendReportLocation(
            location,
            reasonForReport = "Automatic graph location error: $userFacingReason",
            additionalInternalInfo = additionalInternalInfo,
            ignoreCache = ignoreCache,
            betaOnly = betaOnly,
        )
    }

    private fun sendReportLocation(
        location: LorenzVec,
        reasonForReport: String,
        userReason: String? = null,
        additionalInternalInfo: String? = null,
        ignoreCache: Boolean,
        betaOnly: Boolean,
    ) {
        val graphArea = SkyBlockUtils.graphArea
        val scoreboardArea = SkyBlockUtils.scoreboardArea ?: "unknown"

        val extraData = mutableMapOf<String, Any>()
        userReason?.let {
            extraData["reason provided by user"] = it
        }
        additionalInternalInfo?.let {
            extraData["internal info"] = it
        }
        val island = SkyBlockUtils.currentIsland.name
        extraData["island"] = island
        extraData["location"] = with(location.roundTo(1)) { "/shtestwaypoint $x $y $z pathfind" }
        if (graphArea != scoreboardArea) {
            extraData["area graph"] = graphArea.orEmpty()
            extraData["area scoreboard"] = scoreboardArea
        }

        SkyHanniRepoManager.localRepoCommit.let { (hash, time) ->
            extraData["repo update time"] = time?.toString() ?: "none"
            extraData["repo update age"] = time?.passedSince() ?: "unknown"
            extraData["repo update hash"] = hash ?: "none"
        }

        ErrorManager.logErrorStateWithData(
            reasonForReport,
            "",
            noStackTrace = true,
            extraData = extraData.map { it.key to it.value }.normalizeAsArray(),
            ignoreErrorCache = ignoreCache,
            betaOnly = betaOnly,
        )
    }

}
