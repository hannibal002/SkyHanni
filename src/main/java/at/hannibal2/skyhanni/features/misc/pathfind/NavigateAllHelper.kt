package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object NavigateAllHelper {

    private const val NAVIGATE_AGAIN_DISTANCE = 5
    private const val CLIPBOARD_TARGET_NAME = "Clipboard Waypoint"
    private val defaultClipboardWaitTime = 5.seconds

    private val allowedMultiNavigationTags = setOf(
        GraphNodeTag.HOPPITY,
        GraphNodeTag.RIFT_EFFIGY,
        GraphNodeTag.RIFT_MONTEZUMA,
        GraphNodeTag.CRIMSON_MINIBOSS,
        GraphNodeTag.SPIDER_RELIC,
        GraphNodeTag.END_GOLEM,
        GraphNodeTag.FISHING_HOTSPOT,
        GraphNodeTag.FISHING_WORMHOLE,
        GraphNodeTag.FAIRY_SOUL,
        GraphNodeTag.HIDEONLEAF,
        GraphNodeTag.HIDEONSUN,
        GraphNodeTag.TREE_PROTECTION_ORDER,
        GraphNodeTag.HONEYHIVE,
        GraphNodeTag.SAFARI_BELL,
        GraphNodeTag.HIDEYHO_LOCATION,
        GraphNodeTag.PANGOLIN,
        GraphNodeTag.SANGER,
        GraphNodeTag.FLOOR_DROPS,
    )

    private val pathfindCoroutine = CoroutineSettings("navigate all pathfind")

    private val currentlyNavigating get() = currentTargetName != null

    private var route: List<GraphNode> = listOf()
    private var total = 0
    private var currentTarget: GraphNode? = null
    private var currentTargetName: String? = null
    private var color = LorenzColor.WHITE.toColor()
    private var waitingOnCondition: Boolean = false
    private var optimizeRoute = true
    private var renderWaypoint = false

    private var onFound: (GraphNode) -> Unit = {}
    private var onFinish: () -> Unit = {}
    private var continueNavigationCondition: NavigationCondition = NavigationCondition.None
    private var condition: () -> Boolean = { true }

    /**
     * Navigate to all nodes with the selected [GraphNodeTag]
     */
    private fun navigateAllCommand(nodeType: GraphNodeTag) {
        if (nodeType !in getValidTagNames()) {
            ChatUtils.userError("${nodeType.displayName} §cis invalid for navigation on this island!")
            return
        }

        val graph = IslandGraphs.currentIslandGraph ?: return
        val targetNodes = graph.getNodesWithTags(nodeType)

        navigateAll(
            targetNodes,
            nodeType.displayName,
            nodeType.color.toColor(),
            onFinish = { ChatUtils.chat("Reached all ${StringUtils.pluralize(total, nodeType.displayName, withNumber = true)}§e.") },
            continueNavigationCondition = NavigationCondition.None,
            condition = { true },
        )
    }

    /**
     * Navigate to all locations read from the clipboard, one `x:y:z` per line, in the order they were pasted.
     *
     * The player has to stay within [NAVIGATE_AGAIN_DISTANCE] blocks of a waypoint for [waitTime] before the
     * navigation moves on to the next one. Walking away restarts that timer, since the pathfinder navigates
     * back to the same waypoint first.
     *
     * @param waitTime How long the player has to stand at a waypoint before the next one is targeted.
     */
    private fun navigateAllClipboardCommand(waitTime: Duration) {
        if (IslandGraphs.currentIslandGraph == null) {
            ChatUtils.userError("There is no path finder network on this island.")
            return
        }

        val clipboard = ClipboardUtils.readFromClipboard().orEmpty()
        if (clipboard.isBlank()) {
            ChatUtils.userError("Your clipboard is empty. Copy one §ex:y:z §clocation per line first.")
            return
        }

        val lines = clipboard.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val locations = lines.mapNotNull { parseLocationOrNull(it) }
        if (locations.isEmpty()) {
            ChatUtils.userError("Could not read any location from your clipboard. Expected one §ex:y:z §cper line.")
            return
        }

        // The ids are only used as the waypoint number shown in the world. These nodes never reach the island
        // graph, they are exclusively read by recursiveNavigate, which only uses their position.
        val nodes = locations.mapIndexed { index, location -> GraphNode(id = index + 1, position = location) }

        val skippedLines = lines.size - locations.size
        val skippedSuffix = if (skippedLines > 0) " §7($skippedLines unreadable)" else ""
        ChatUtils.chat("Read ${StringUtils.pluralize(nodes.size, "location", withNumber = true)} from the clipboard.$skippedSuffix")

        var arrivedAt = SimpleTimeMark.farPast()

        navigateAll(
            nodes,
            CLIPBOARD_TARGET_NAME,
            LorenzColor.AQUA.toColor(),
            onFound = { arrivedAt = SimpleTimeMark.now() },
            onFinish = {
                renderWaypoint = false
                ChatUtils.chat("Reached all ${StringUtils.pluralize(total, CLIPBOARD_TARGET_NAME, withNumber = true)}§e.")
            },
            continueNavigationCondition = NavigationCondition.SecondPassed { arrivedAt.passedSince() >= waitTime },
            condition = { true },
            optimizeRoute = false,
            renderWaypoint = true,
        )
    }

    private fun parseLocationOrNull(line: String): LorenzVec? {
        val parts = line.split(":")
        if (parts.size != 3) return null
        val (x, y, z) = parts.map { it.trim().toDoubleOrNull() ?: return null }
        return LorenzVec(x, y, z)
    }

    /**
     * Navigate to all the inputted nodes.
     *
     * @param nodes The list of nodes that should be navigated to.
     * @param targetName The name of what is being navigated to.
     * @param color The color of the pathfinding line.
     * @param onFound What should be done upon reaching the location of a node.
     * @param onFinish What should be done upon reaching all nodes.
     * @param continueNavigationCondition The condition that must be met before moving to the next node.
     * @param condition The condition for the navigation to be shown.
     * @param optimizeRoute Whether the nodes should be reordered into the shortest route. When false, the nodes
     *  are visited in the order they were passed in. Required for nodes that are not part of the island graph,
     *  since the route calculation needs their neighbors.
     * @param renderWaypoint Whether a filled block with the node id should be drawn at the current target.
     *
     * Existing features should be switched to use a more abstract version of this
     * These features include: Fast Fairy Souls, Spider Relic Pathfind, Shulker Finder
     */
    fun navigateAll(
        nodes: List<GraphNode>,
        targetName: String,
        color: Color,
        onFound: (GraphNode) -> Unit = {},
        onFinish: () -> Unit,
        continueNavigationCondition: NavigationCondition,
        condition: () -> Boolean,
        optimizeRoute: Boolean = true,
        renderWaypoint: Boolean = false,
    ) {
        currentTargetName = targetName
        this.color = color
        this.onFound = onFound
        this.onFinish = onFinish
        this.continueNavigationCondition = continueNavigationCondition
        this.condition = condition
        this.optimizeRoute = optimizeRoute
        this.renderWaypoint = renderWaypoint

        // Coroutine for calculateRoute()
        pathfindCoroutine.launch {
            route = if (optimizeRoute) calculateRoute(nodes) else nodes
            total = route.size

            ChatUtils.chat("§aNavigating to ${StringUtils.pluralize(total, targetName, withNumber = true)}§a.")
            ChatUtils.chat("§aUse §e/shnavall skip §ato skip a target.")

            recursiveNavigate()
        }
    }

    private fun recursiveNavigate() {
        waitingOnCondition = false

        if (route.isEmpty()) {
            onFinish()
            currentTargetName = null
            return
        }

        val target = route.first()
        currentTarget = target
        route = route.drop(1)

        IslandGraphs.pathFind(
            target.position,
            "$currentTargetName ${total - route.size}/$total",
            color = color,
            onFound = {
                onFound(target)

                when (continueNavigationCondition) {
                    NavigationCondition.None -> recursiveNavigate()
                    is NavigationCondition.ChatMessage -> waitingOnCondition = true
                    is NavigationCondition.SecondPassed -> {
                        if ((continueNavigationCondition as NavigationCondition.SecondPassed).condition(target)) {
                            recursiveNavigate()
                        } else {
                            if (currentlyNavigating) {
                                waitingOnCondition = true
                            }
                        }
                    }
                }
            },
            condition = { currentlyNavigating && condition() },
        )
    }

    private fun calculateRoute(targetNodes: List<GraphNode>): List<GraphNode> = NavigationUtils.getRoute(targetNodes)

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!renderWaypoint || !currentlyNavigating) return
        val target = currentTarget ?: return

        event.drawWaypointFilled(target.position, color, seeThroughBlocks = true)
        event.drawString(target.position.add(0.5, 1.5, 0.5), "§e${target.id}", seeThroughBlocks = true)
    }

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!waitingOnCondition) return
        val messageCondition = (continueNavigationCondition as? NavigationCondition.ChatMessage)?.condition ?: return

        if (messageCondition(event.cleanMessage)) {
            recursiveNavigate()
        }
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!waitingOnCondition) return

        val target = currentTarget ?: return

        if (currentlyNavigating && (target.position.distanceToPlayer()) > NAVIGATE_AGAIN_DISTANCE) {
            route = listOf(target) + route
            recursiveNavigate()
            return
        }

        val secondPassedCondition = (continueNavigationCondition as? NavigationCondition.SecondPassed)?.condition ?: return

        if (secondPassedCondition(target)) {
            recursiveNavigate()
        }
    }

    private fun handleSkip() {
        if (!currentlyNavigating) {
            ChatUtils.userError("No current navigation to skip. §eUse /shnavigateall to start navigation")
            return
        }

        ChatUtils.chat("Skipping a $currentTargetName§e.")

        if (!optimizeRoute) {
            recursiveNavigate()
            return
        }

        pathfindCoroutine.launch {
            route = calculateRoute(route)
            recursiveNavigate()
        }
    }

    fun handleStop(manual: Boolean = false) {
        if (!currentlyNavigating) {
            ChatUtils.userError("No current navigation to stop. §eUse /shnavigateall to start navigation")
            return
        }

        if (manual) {
            ChatUtils.userError("Manually stopped navigation")
        }

        route = emptyList()
        currentTargetName = null
        waitingOnCondition = false
        renderWaypoint = false

        IslandGraphs.stopNavigation()
    }

    @HandleEvent
    private fun onIslandLeave() {
        route = emptyList()
        currentTargetName = null
        waitingOnCondition = false
        renderWaypoint = false
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavigateall") {
            description = "Use the path finder to go to all locations of a type"
            aliases = listOf("shnavall")

            argCallback(
                "nodeType",
                EnumArgumentType.filtered<GraphNodeTag>(
                    { it.cleanName.replace(" ", "_") },
                    isGreedy = true,
                ) { it in allowedMultiNavigationTags },
                BrigadierUtils.dynamicSuggestionProvider { getValidTagNames().map { it.cleanName } },
            ) { nodeType ->
                navigateAllCommand(nodeType)
            }
            literalCallback("skip") {
                handleSkip()
            }
            literalCallback("stop") {
                handleStop(manual = true)
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shnavigateall <location type>")
            }
        }
        event.registerBrigadier("shnavigateallclipboard") {
            description = "Use the path finder to go to all locations read from the clipboard"
            category = CommandCategory.DEVELOPER_TEST

            argCallback("seconds", BrigadierArguments.integer(min = 0)) { seconds ->
                navigateAllClipboardCommand(seconds.seconds)
            }
            literalCallback("skip") {
                handleSkip()
            }
            literalCallback("stop") {
                handleStop(manual = true)
            }
            simpleCallback {
                navigateAllClipboardCommand(defaultClipboardWaitTime)
            }
        }
    }

    private fun getValidTagNames(): Set<GraphNodeTag> {
        val activeTags = IslandGraphs.currentIslandGraph?.getActiveNodeTags() ?: return emptySet()
        return activeTags.filter { it in allowedMultiNavigationTags }.toSet()
    }
}
