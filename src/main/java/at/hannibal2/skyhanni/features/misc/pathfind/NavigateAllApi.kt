package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.getBoxCenter
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.phys.AABB
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

/**
 * Drives a navigation over a list of nodes, one after the other.
 *
 * Holds the state of the single navigation that can run at a time. Features start one via `navigateAll` and
 * add their own command entry points that delegate to `handleSkip`, `handleStop` and `handleUndo`.
 *
 * The route stays untouched once it is calculated, the position inside it is tracked by an index. Skipping and
 * going back therefore only move that index, and a skipped node can be returned to.
 */
@SkyHanniModule
object NavigateAllApi {

    private const val NAVIGATE_AGAIN_DISTANCE = 5
    private const val NODE_REACHED_DISTANCE = 3.0
    private const val CLICK_RANGE = 5.0

    private val unreachableWarningDelay = 10.seconds
    private val pathfindCoroutine = CoroutineSettings("navigate all pathfind")

    private val defaultTargetLocation: (GraphNode) -> LorenzVec = { it.position }

    private val currentlyNavigating get() = currentTargetName != null
    private val currentTarget get() = route.getOrNull(currentIndex)

    private var navigationId = 0
    private var route: List<GraphNode> = listOf()
    private var currentIndex = 0
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
     * @param usageHint The line shown to the player after the navigation started, explaining how to control it.
     *  The default names the skip command, which works for every caller because they all share one navigation.
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
        usageHint: String = "§aUse §e/shnavall skip §ato skip a target.",
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

        route = emptyList()
        currentIndex = 0
        val id = ++navigationId

        // Coroutine for calculateRoute()
        pathfindCoroutine.launch {
            val newRoute = if (optimizeRoute) calculateRoute(nodes) else nodes
            if (id != navigationId) return@launch

            route = newRoute

            ChatUtils.chat("§aNavigating to ${StringUtils.pluralize(route.size, targetName, withNumber = true)}§a.")
            ChatUtils.chat(usageHint)

            navigateToCurrent()
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
            advance()
            return
        }

        val visited = route.take(currentIndex + 1)
        val remaining = route.drop(currentIndex + 1)

        pathfindCoroutine.launch {
            val newRemaining = calculateRoute(remaining)
            if (id != navigationId) return@launch

            route = visited + newRemaining
            advance()
        }
    }

    fun handleUndo() {
        if (!currentlyNavigating) {
            ChatUtils.userError("No current navigation to go back in. §eUse /shnavigateall to start navigation")
            return
        }

        if (currentIndex == 0) {
            ChatUtils.userError("Already at the first $currentTargetName§c.")
            return
        }

        currentIndex--
        ChatUtils.chat("Going back to $currentTargetName ${currentIndex + 1}/${route.size}§e.")
        navigateToCurrent()
    }

    fun handleStop(manual: Boolean = false, errorMessage: Boolean = true) {
        if (!currentlyNavigating) {
            if (errorMessage) {
                ChatUtils.userError("No current navigation to stop. §eUse /shnavigateall to start navigation")
            }
            return
        }

        if (manual) {
            ChatUtils.userError("Manually stopped navigation")
        }

        resetState()

        IslandGraphs.stopNavigation()
    }

    private fun advance() {
        currentIndex++
        navigateToCurrent()
    }

    private fun navigateToCurrent() {
        waitingOnCondition = false

        val target = currentTarget ?: run {
            onFinish()
            resetState()
            return
        }

        unreachableWarningSent = false
        nodeReachedAt = null

        IslandGraphs.pathFind(
            targetLocation(target),
            "$currentTargetName ${currentIndex + 1}/${route.size}",
            color = color,
            onFound = {
                onFound(target)

                when (continueNavigationCondition) {
                    NavigationCondition.None -> advance()
                    NavigationCondition.Manual -> waitingOnCondition = true
                    is NavigationCondition.ChatMessage -> waitingOnCondition = true
                    is NavigationCondition.SecondPassed -> {
                        if ((continueNavigationCondition as NavigationCondition.SecondPassed).condition(target)) {
                            advance()
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
        val index = currentIndex
        ChatUtils.clickableChat(
            "Could not get to $currentTargetName ${index + 1}/${route.size}§e. Click to skip it.",
            onClick = { if (currentIndex == index) handleSkip() },
            hover = "§eClick to skip this waypoint!",
            oneTimeClick = true,
        )
    }

    private fun resetState() {
        navigationId++
        route = emptyList()
        currentIndex = 0
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
    private fun onItemClick(event: ItemClickEvent) {
        if (event.clickType != InteractClickType.LEFT_CLICK) return
        if (continueNavigationCondition != NavigationCondition.Manual) return
        if (!currentlyNavigating) return
        val target = currentTarget ?: return

        val location = targetLocation(target)
        val box = AABB(location.x, location.y, location.z, location.x + 1, location.y + 1, location.z + 1)
        if (box.getBoxCenter().distanceToPlayer() > CLICK_RANGE) return

        val direction = MinecraftCompat.localPlayerOrThrow.lookAngle.toLorenzVec()
        if (!box.rayIntersects(LocationUtils.playerEyeLocation(), direction)) return

        event.cancel()
        advance()
    }

    @HandleEvent
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!renderNumber || !currentlyNavigating) return
        val target = currentTarget ?: return

        event.drawString(targetLocation(target).add(0.5, 1.5, 0.5), "§e${currentIndex + 1}", seeThroughBlocks = true)
    }

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!waitingOnCondition) return
        val messageCondition = (continueNavigationCondition as? NavigationCondition.ChatMessage)?.condition ?: return

        if (messageCondition(event.cleanMessage)) {
            advance()
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
            navigateToCurrent()
            return
        }

        val secondPassedCondition = (continueNavigationCondition as? NavigationCondition.SecondPassed)?.condition ?: return

        if (secondPassedCondition(target)) {
            advance()
        }
    }

    @HandleEvent
    private fun onIslandLeave() {
        resetState()
    }
}
