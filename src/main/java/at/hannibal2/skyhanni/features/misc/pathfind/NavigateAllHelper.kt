package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import java.awt.Color

@SkyHanniModule
object NavigateAllHelper {

    private const val NAVIGATE_AGAIN_DISTANCE = 5

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
    )

    private val pathfindCoroutine = CoroutineSettings("navigate all pathfind")

    private val currentlyNavigating get() = currentTargetName != null

    private var route: List<GraphNode> = listOf()
    private var total = 0
    private var currentTarget: GraphNode? = null
    private var currentTargetName: String? = null
    private var color = LorenzColor.WHITE.toColor()
    private var waitingOnCondition: Boolean = false

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
     * Navigate to all the inputted nodes.
     *
     * @param nodes The list of nodes that should be navigated to.
     * @param targetName The name of what is being navigated to.
     * @param color The color of the pathfinding line.
     * @param onFound What should be done upon reaching the location of a node.
     * @param onFinish What should be done upon reaching all nodes.
     * @param continueNavigationCondition The condition that must be met before moving to the next node.
     * @param condition The condition for the navigation to be shown.
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
    ) {
        currentTargetName = targetName
        this.color = color
        this.onFound = onFound
        this.onFinish = onFinish
        this.continueNavigationCondition = continueNavigationCondition
        this.condition = condition

        // Coroutine for calculateRoute()
        pathfindCoroutine.launch {
            route = calculateRoute(nodes)
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
                            waitingOnCondition = true
                        }
                    }
                }
            },
            condition = { currentlyNavigating && condition() },
        )
    }

    private fun calculateRoute(targetNodes: List<GraphNode>): List<GraphNode> = NavigationUtils.getRoute(targetNodes)

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

        if (currentlyNavigating && (currentTarget?.position?.distanceToPlayer() ?: 0.0) > NAVIGATE_AGAIN_DISTANCE) {
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

        pathfindCoroutine.launch {
            route = calculateRoute(route)
            recursiveNavigate()
        }
    }

    fun handleStop() {
        if (!currentlyNavigating) {
            ChatUtils.userError("No current navigation to stop. §eUse /shnavigateall to start navigation")
            return
        }

        route = emptyList()
        currentTargetName = null
        waitingOnCondition = false

        IslandGraphs.stopNavigation()
    }

    @HandleEvent
    private fun onIslandLeave() {
        route = emptyList()
        currentTargetName = null
        waitingOnCondition = false
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
                handleStop()
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shnavigateall <location type>")
            }
        }
    }

    private fun getValidTagNames(): Set<GraphNodeTag> {
        val activeTags = IslandGraphs.currentIslandGraph?.getActiveNodeTags() ?: return emptySet()
        return activeTags.filter { it in allowedMultiNavigationTags }.toSet()
    }
}
