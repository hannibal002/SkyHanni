package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.jsonobjects.repo.IslandGraphSettingsJson
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.data.navigation.PathRenderer
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaFeatures
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationFeedback
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.LocalPlayer
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * TODO
 * benefits of every island graphs:
 * global:
 * 	races (end, park, winter, dungeon hub)
 * 	jump pads between servers
 * 	ring of love/romeo juliet quest
 * 	death location
 * 	% of island discovered (every node was most closest node at least once)
 * hub:
 * 	diana
 * farming:
 * 	pelt farming area
 * rift:
 * 	eyes
 * 	big quests
 * 	blood effigies
 * spider:
 * 	relicts + throw spot
 * dwarven mines:
 * 	emissary
 * 	commission areas
 * 	events: raffle, goblin slayer, donpieresso
 * deep
 * 	path to the bottom (Rhys NPC) (replace in DeepCavernsGuide.kt)
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
 * 	toggle distance to node by node path length, instead of eye of sight length
 * 	press test button again to enable "true test mode", with hiding other stuff
 * 	option to compare two graphs, and store multiple graphs in the edit mode in parallel
 */

@SkyHanniModule
object IslandGraphs {

    private const val TARGET_REACHED_DISTANCE_SQ = 9.0
    private const val ON_PATH_MAX_DISTANCE_SQ = 49.0
    private const val FAST_MOVEMENT_THRESHOLD = 20.0
    private const val NEARBY_NODE_CACHE_SIZE = 20
    private const val DEFAULT_NODE_SEARCH_RADIUS = 100.0
    private val GRAPH_RELOAD_DELAY = 500.milliseconds
    private val CLOSEST_NODE_CACHE_TTL = 1.seconds

    var currentIslandGraph: Graph? = null
        private set
    var lastLoadedIslandType = "nothing"
    private var lastLoadedTime = SimpleTimeMark.farPast()

    var disabledNodesReason: String? = null
        private set

    fun disableNodes(reason: String, center: LorenzVec, radius: Double) {
        disabledNodesReason = reason
        for (node in getGraph().filter { it.position.distance(center) < radius }) {
            node.enabled = false
        }
    }

    fun enableAllNodes() {
        disabledNodesReason = null
        val graph = currentIslandGraph ?: return
        graph.forEach { it.enabled = true }
    }

    var closestNode: GraphNode? = null
        private set

    private var cachedNearbyNodes = listOf<GraphNode>()
    private var lastCacheUpdate = SimpleTimeMark.farPast()

    private var currentTarget: LorenzVec? = null
    private var currentTargetNode: GraphNode? = null
    private var navigationLabel = ""
    private var lastDisplayedDistance = 0.0
    private var totalDistance = 0.0
    private var pathColor = Color.WHITE
    private var allowRerouting = false
    private var onFound: () -> Unit = {}
    private var onManualCancel: () -> Unit = {}
    private var goal: GraphNode? = null
        set(value) {
            prevGoal = field
            field = value
        }
    private var prevGoal: GraphNode? = null

    private var pathRenderer: PathRenderer? = null
    private var activeCondition: () -> Boolean = { true }
    private var inGlaciteTunnels: Boolean? = null
    private var ignoredIslandTypes = setOf<IslandType>()

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

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<IslandGraphSettingsJson>("misc/IslandGraphSettings")
        ignoredIslandTypes = data.ignoredIslandTypes

        if (SkyBlockUtils.inSkyBlock) {
            loadIsland(SkyBlockUtils.currentIsland)
        }
    }

    @HandleEvent
    fun onIslandJoin(event: IslandJoinEvent) {
        enableAllNodes()
        if (currentIslandGraph != null) return
        if (event.island == IslandType.NONE) return
        loadIsland(event.island)
    }

    @HandleEvent
    fun onWorldChange() {
        currentIslandGraph = null
        if (currentTarget != null) NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation stopped because of world switch!")
        resetNavigation()
    }

    private fun isGlaciteTunnelsArea(area: String?): Boolean = glaciteTunnelsPattern.matches(area)

    @HandleEvent(ScoreboardAreaChangeEvent::class)
    fun onAreaChange() {
        if (!IslandType.DWARVEN_MINES.isInIsland()) {
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
            return
        }

        // TODO custom behaviour for mineshaft or catacombs, private island, or garden

        if (newIsland in ignoredIslandTypes) return

        reloadFromJson(newIsland.name)
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Island Graphs")
        val islandType = SkyBlockUtils.currentIsland.name
        val isPersonal = IslandTypeTag.PERSONAL_ISLAND.isInIsland()
        val important = SkyBlockUtils.inSkyBlock && lastLoadedIslandType != islandType && !isPersonal
        val list = buildList {
            add("")
            if (important) {
                add("wrong island!")
            } else {
                if (isPersonal) {
                    add("disabled on $islandType")
                } else {
                    add("island is correct!")
                }
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
        CoroutineSettings("load island graph data for $islandName").launchCoroutine {
            try {
                val graph = SkyHanniRepoManager.getRepoDataAsync<Graph>("constants/island_graphs", islandName, gson = Graph.gson)
                IslandAreaFeatures.display = null
                DelayedRun.runNextTick {
                    setNewGraph(graph)
                }
            } catch (e: Error) {
                currentIslandGraph = null
                if (SkyBlockUtils.debug) {
                    ErrorManager.logErrorWithData(
                        e,
                        "failed to load graph data for island $islandName",
                        "island name" to islandName,
                    )
                }
            }
        }
    }

    fun setNewGraph(graph: Graph) {
        currentIslandGraph = graph
        if (currentTarget != null) {
            DelayedRun.runDelayed(GRAPH_RELOAD_DELAY) {
                processNavigation()
                handleMovementUpdate()
            }
        }

        // calling various update functions to make switching between deep caverns and glacite tunnels bearable
        processNavigation()
        IslandGraphReloadEvent(graph).post()
    }

    private fun resetNavigation() {
        stopNavigation()
        closestNode = null
        cachedNearbyNodes = emptyList()
        lastCacheUpdate = SimpleTimeMark.farPast()
    }

    /**
     * calling before [at.hannibal2.skyhanni.test.graph.GraphEditor], so that we always have the latest playerPosition.
     */
    @HandleEvent(priority = -1)
    fun onTick(event: SkyHanniTickEvent) {
        GraphUtils.updatePlayerPosition()
        if (currentIslandGraph == null) return
        if (event.isMod(2)) {
            // TODO add dev config toggle to disable
            refreshNavigation()
        }
        updateNavigationProgress()
    }

    fun refreshNavigation(force: Boolean = false) {
        if (force) {
            closestNode = null
        }
        processNavigation()
        handleMovementUpdate()
        pathRenderer?.updateNearSegment()
    }

    private fun processNavigation() {
        GraphUtils.updatePlayerPosition()
        currentTarget?.let {
            if (distanceSqToPlayer(it) < TARGET_REACHED_DISTANCE_SQ) {
                NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation reached §r$navigationLabel§e!")
                resetNavigation()
                onFound()
            }
            if (!activeCondition()) {
                resetNavigation()
            }
        }

        // Update cache every second for normal movement
        if (lastCacheUpdate.passedSince() > CLOSEST_NODE_CACHE_TTL) {
            updateClosestCache(getGraph())
        }

        val newClosest = cachedNearbyNodes.minByOrNull { it.distanceSqToPlayer() } ?: return
        if (closestNode == newClosest) return
        val newPath = !onCurrentPath()

        closestNode = newClosest
        onClosestNodeChanged()
        if (newPath) {
            findNewPath()
        }
    }

    private fun updateClosestCache(graph: Graph) {
        cachedNearbyNodes = graph.sortedBy { it.distanceSqToPlayer() }.take(NEARBY_NODE_CACHE_SIZE)
        lastCacheUpdate = SimpleTimeMark.now()
    }

    private fun onCurrentPath(): Boolean {
        val renderer = pathRenderer ?: return false
        return renderer.nearestPathDistanceSq() <= ON_PATH_MAX_DISTANCE_SQ
    }

    private fun findNewPath() {
        val goal = goal ?: return
        val closest = closestNode ?: return

        val (path, distance) = GraphUtils.findShortestPathAsGraphWithDistance(closest, goal)
        val first = path.firstOrNull()
        val second = path.getOrNull(1)

        val nodeDistance = first?.distanceToPlayer() ?: 0.0
        if (first != null && second != null) {
            val direct = second.distanceToPlayer()
            val firstPath = first.neighbors[second] ?: 0.0
            val around = nodeDistance + firstPath
            if (direct < around) {
                applyPath(Graph(path.drop(1)) to (distance - firstPath + direct))
                return
            }
        }
        applyPath(path to (distance + nodeDistance))
    }

    private var hasMoved = false

    private fun handleMovementUpdate() {
        if (!hasMoved) return
        hasMoved = false
        if (goal != null) {
            updateNavigationProgress()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        val graph = currentIslandGraph
        if (graph == null || !event.isLocalPlayer) return
        hasMoved = true

        if (event.distance > FAST_MOVEMENT_THRESHOLD) {
            updateClosestCache(graph)
        }
    }

    private fun applyPath(path: Pair<Graph, Double>, setPath: Boolean = true) {
        val (graph, _) = path.takeIf { it.first.isNotEmpty() } ?: return
        setupPathRenderer(setPath, graph.toList())
    }

    private fun setupPathRenderer(setPath: Boolean = true, nodes: List<GraphNode>) {
        if (setPath) {
            pathRenderer = PathRenderer(Graph(nodes), pathColor, currentTarget ?: error("target is null"))
            pathRenderer?.updateNearSegment()
        }
        updateNavigationProgress()
    }

    private fun onClosestNodeChanged() {
        // TODO create an event
        IslandAreaBackend.nodeMoved()
        if (allowRerouting) {
            tryRerouting()
        }
    }

    private fun tryRerouting() {
        val target = currentTargetNode ?: return
        val closest = closestNode ?: return
        val map = GraphUtils.findAllShortestDistances(closest).distances.filter { it.key.sameNameAndTags(target) }
        val newTarget = map.sorted().keys.firstOrNull() ?: return
        if (newTarget != target) {
            ChatUtils.debug("Rerouting navigation..")
            newTarget.pathFind(navigationLabel, pathColor, onFound, allowRerouting = true, condition = activeCondition)
        }
    }

    fun stopNavigation() {
        if (currentTarget != null) {
            NavigationFeedback.sendPathFindMessage("§e[SkyHanni] Navigation stopped!")
            currentTarget = null
        }
        goal = null
        pathRenderer = null
        currentTargetNode = null
        navigationLabel = ""
        totalDistance = 0.0
        lastDisplayedDistance = 0.0
        NavigationFeedback.setNavInactive()
    }

    /**
     * Activates pathfinding, with this graph node as goal.
     *
     * @param label The name of the navigation goal in chat.
     * @param color The color of the lines in world.
     * @param onFound The callback that gets fired when the goal is reached.
     * @param allowRerouting When a different node with same name and tags as the original goal is closer to the player, starts routing to this instead.
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
        resetNavigation()
        currentTargetNode = this
        IslandGraphs.allowRerouting = allowRerouting
        initNavigation(location = position, label, color, onFound, onManualCancel, condition)
    }

    /**
     * Activates pathfinding to a location on the current island.
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
        resetNavigation()
        allowRerouting = false
        initNavigation(location, label, color, onFound, onManualCancel, condition)
    }

    private fun getGraph(): Graph = currentIslandGraph ?: error("current island graph is not loaded")

    fun node(nodeName: String, nodeTag: GraphNodeTag): GraphNode =
        getGraph().getClosestNode(nodeName, nodeTag) ?: error("node not found: name: '$nodeName', tag: '$nodeTag'")

    fun nodes(nodeName: String, nodeTag: GraphNodeTag): List<GraphNode> =
        getGraph().getNodesWithNameAndTags(nodeName, nodeTag)

    fun nodesAround(node: GraphNode, condition: (GraphNode) -> Boolean): Set<GraphNode> =
        getGraph().nodesAround(node, condition)

    private fun initNavigation(
        location: LorenzVec,
        label: String,
        color: Color = LorenzColor.WHITE.toColor(),
        onFound: () -> Unit = {},
        onManualCancel: () -> Unit = {},
        condition: () -> Boolean,
    ) {
        currentTarget = location
        this.navigationLabel = label
        this.pathColor = color
        this.onFound = onFound
        this.onManualCancel = onManualCancel
        this.activeCondition = condition
        goal = getGraph().minByActive { it.position.distance(currentTarget!!) }
        updateNavigationProgress()
    }

    private fun updateNavigationProgress() {
        if (navigationLabel == "") return
        val distance = (pathRenderer?.remainingDistance ?: return).roundTo(1)

        if (distance == lastDisplayedDistance) return
        lastDisplayedDistance = distance
        if (distance == 0.0) return
        if (totalDistance == 0.0 || distance > totalDistance) {
            totalDistance = distance
        }

        val percentage = (1 - (distance / totalDistance)) * 100
        val component = "§e[SkyHanni] Navigating to §r$navigationLabel §f[§e$distance§f] §f(§c${percentage.roundTo(1)}%§f)".asComponent()
        component.onClick(onClick = ::cancelClick)
        component.hover = "§eClick to stop navigating!".asComponent()
        NavigationFeedback.sendPathFindMessage(component)
    }

    fun cancelClick() {
        stopNavigation()
        onManualCancel()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (currentIslandGraph == null) return
        pathRenderer?.render(event)
    }

    fun isActive(testTarget: LorenzVec, testLabel: String): Boolean = testTarget == currentTarget && testLabel == navigationLabel

    fun findClosestNode(location: LorenzVec, condition: (GraphNode) -> Boolean, radius: Double = DEFAULT_NODE_SEARCH_RADIUS): GraphNode? {
        val found = getGraph().getNearestNode(location, condition)
        return found.takeIf { it.position.distance(location) < radius }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shreportlocation") {
            description = "Allows the user to report an error with pathfinding at the current location."
            category = CommandCategory.USERS_BUG_FIX
            argCallback("reason", BrigadierArguments.greedyString()) { reason ->
                sendReportLocation(
                    playerPosition,
                    reasonForReport = reason,
                    technicalInfo = "Manual reported graph location error via /shreportlocation",
                    "reason provided by user" to reason,
                )
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shreportlocation <reason>")
                ChatUtils.chat(
                    "Give a reason that explains what's wrong at this location, e.g.: " +
                        "pathfinding goes through wall, ignores obvious shortcut, " +
                        "missing npc/fishing hotspot/skyblock area name in /shnavigate..",
                )
            }
        }
        event.registerBrigadier("shstopnavigation") {
            description = "Stops the current pathfinding."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                if (currentTarget != null) {
                    stopNavigation()
                } else {
                    ChatUtils.userError("No navigation is currently active.")
                }
            }
        }
    }

    fun reportLocation(
        location: LorenzVec,
        userFacingReason: String,
        technicalInfo: String? = null,
        vararg extraData: Pair<String, Any?>,
    ) {
        sendReportLocation(
            location,
            reasonForReport = userFacingReason,
            technicalInfo = "Automatic graph location error: $technicalInfo",
            extraData = extraData,
        )
    }

    private fun sendReportLocation(
        location: LorenzVec,
        reasonForReport: String,
        technicalInfo: String? = null,
        vararg extraData: Pair<String, Any?>,
    ) {
        val graphArea = SkyBlockUtils.graphArea
        val scoreboardArea = SkyBlockUtils.scoreboardArea ?: "unknown"

        val data = mutableMapOf<String, Any?>()
        technicalInfo?.let {
            data["technical info"] = it
        }
        data.putAll(extraData.toMap())
        val island = SkyBlockUtils.currentIsland.name

        data["generic data"] = "below"
        data["island"] = island
        data["reported location"] = "/shtestwaypoint ${location.toLocalFormat()} pathfind"
        if (graphArea != scoreboardArea) {
            data["area graph"] = graphArea.orEmpty()
            data["area scoreboard"] = scoreboardArea
        }

        SkyHanniRepoManager.localRepoCommit.let { (hash, time) ->
            data["repo update time"] = time?.toString() ?: "none"
            data["repo update age"] = time?.passedSince() ?: "unknown"
            data["repo update hash"] = hash ?: "none"
        }

        ErrorManager.logErrorStateWithData(
            reasonForReport,
            "",
            noStackTrace = true,
            extraData = data.map { it.key to it.value }.normalizeAsArray(),
        )
    }
}
