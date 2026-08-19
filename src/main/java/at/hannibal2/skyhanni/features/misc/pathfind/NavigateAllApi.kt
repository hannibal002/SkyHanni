package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi.handleSkip
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi.handleStop
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi.navigateAll
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

/**
 * Drives a navigation over a list of nodes, one after the other.
 *
 * Holds the state of the single navigation that can run at a time. Features start one via `navigateAll` and
 * add their own command entry points that delegate to `handleSkip` and `handleStop`.
 */
@SkyHanniModule
object NavigateAllApi {

    private const val NAVIGATE_AGAIN_DISTANCE = 5
    private const val NODE_REACHED_DISTANCE = 3.0

    private val unreachableWarningDelay = 10.seconds
    private val pathfindCoroutine = CoroutineSettings("navigate all pathfind")

    private val defaultTargetLocation: (GraphNode) -> LorenzVec = { it.position }

    private val currentlyNavigating get() = currentTargetName != null

    private var navigationId = 0
    private var route: List<GraphNode> = listOf()
    private var total = 0
    private var currentTarget: GraphNode? = null
    private var currentTargetName: String? = null
    private var color = LorenzColor.WHITE.toColor()
    private var waitingOnCondition: Boolean = false
    private var optimizeRoute = true
    private var renderNumber = false
    private var warnWhenUnreachable = false
    private var unreachableWarningSent = false
    private var nodeReachedAt: SimpleTimeMark? = null
    private var targetLocation: (GraphNode) -> LorenzVec = defaultTargetLocation

    private var onFound: (GraphNode) -> Unit = {}
    private var onFinish: () -> Unit = {}
    private var continueNavigationCondition: NavigationCondition = NavigationCondition.None
    private var condition: () -> Boolean = { true }

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
     *  are visited in the order they were passed in.
     * @param renderNumber Whether the position of the node in the route should be drawn at the current target.
     * @param warnWhenUnreachable Offer a clickable skip once the player stands at the node but can not get to
     *  [targetLocation]. Only useful together with a [targetLocation] that differs from the node position.
     * @param targetLocation The location that is navigated to for a node, by default the node itself. Callers
     *  with locations outside the island graph map them to the closest node and pass the original location here.
     * @param skipCommand The command shown to the player for skipping a target. Every command that delegates to
     *  `handleSkip` works, so the default fits callers without a command of their own.
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
        renderNumber: Boolean = false,
        warnWhenUnreachable: Boolean = false,
        targetLocation: (GraphNode) -> LorenzVec = defaultTargetLocation,
        skipCommand: String = "/shnavall skip",
    ) {
        currentTargetName = targetName
        this.color = color
        this.onFound = onFound
        this.onFinish = onFinish
        this.continueNavigationCondition = continueNavigationCondition
        this.condition = condition
        this.optimizeRoute = optimizeRoute
        this.renderNumber = renderNumber
        this.warnWhenUnreachable = warnWhenUnreachable
        this.targetLocation = targetLocation

        currentTarget = null
        val id = ++navigationId

        // Coroutine for calculateRoute()
        pathfindCoroutine.launch {
            val newRoute = if (optimizeRoute) calculateRoute(nodes) else nodes
            if (id != navigationId) return@launch

            route = newRoute
            total = route.size

            ChatUtils.chat("§aNavigating to ${StringUtils.pluralize(total, targetName, withNumber = true)}§a.")
            ChatUtils.chat("§aUse §e$skipCommand §ato skip a target.")

            recursiveNavigate()
        }
    }

    fun handleSkip() {
        if (!currentlyNavigating) {
            ChatUtils.userError("No current navigation to skip. §eUse /shnavigateall to start navigation")
            return
        }

        ChatUtils.chat("Skipping a $currentTargetName§e.")

        val id = ++navigationId

        if (!optimizeRoute) {
            recursiveNavigate()
            return
        }

        pathfindCoroutine.launch {
            val newRoute = calculateRoute(route)
            if (id != navigationId) return@launch

            route = newRoute
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

        resetState()

        IslandGraphs.stopNavigation()
    }

    private fun recursiveNavigate() {
        waitingOnCondition = false

        if (route.isEmpty()) {
            onFinish()
            resetState()
            return
        }

        val target = route.first()
        currentTarget = target
        route = route.drop(1)

        unreachableWarningSent = false
        nodeReachedAt = null

        IslandGraphs.pathFind(
            targetLocation(target),
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

    /**
     * Offers a skip when the player stands at the node but does not get to the location behind it.
     */
    private fun checkUnreachable() {
        if (!warnWhenUnreachable || unreachableWarningSent) return
        if (!currentlyNavigating) return
        val target = currentTarget ?: return

        val reachedAt = nodeReachedAt ?: run {
            if (target.position.distanceToPlayer() > NODE_REACHED_DISTANCE) return
            SimpleTimeMark.now().also { nodeReachedAt = it }
        }
        if (reachedAt.passedSince() < unreachableWarningDelay) return

        unreachableWarningSent = true
        ChatUtils.clickableChat(
            "Could not get to $currentTargetName ${total - route.size}/$total§e. Click to skip it.",
            onClick = { if (currentTarget === target) handleSkip() },
            hover = "§eClick to skip this waypoint!",
            oneTimeClick = true,
        )
    }

    private fun resetState() {
        navigationId++
        route = emptyList()
        total = 0
        currentTarget = null
        currentTargetName = null
        color = LorenzColor.WHITE.toColor()
        waitingOnCondition = false
        optimizeRoute = true
        renderNumber = false
        warnWhenUnreachable = false
        unreachableWarningSent = false
        nodeReachedAt = null
        targetLocation = defaultTargetLocation
        onFound = {}
        onFinish = {}
        continueNavigationCondition = NavigationCondition.None
        condition = { true }
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!renderNumber || !currentlyNavigating) return
        val target = currentTarget ?: return

        event.drawString(targetLocation(target).add(0.5, 1.5, 0.5), "§e${total - route.size}", seeThroughBlocks = true)
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
        if (!waitingOnCondition) {
            checkUnreachable()
            return
        }

        val target = currentTarget ?: return

        if (currentlyNavigating && targetLocation(target).distanceToPlayer() > NAVIGATE_AGAIN_DISTANCE) {
            route = listOf(target) + route
            recursiveNavigate()
            return
        }

        val secondPassedCondition = (continueNavigationCondition as? NavigationCondition.SecondPassed)?.condition ?: return

        if (secondPassedCondition(target)) {
            recursiveNavigate()
        }
    }

    @HandleEvent
    private fun onIslandLeave() {
        resetState()
    }
}
