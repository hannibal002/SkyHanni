package at.hannibal2.skyhanni.features.event.lobby.waypoints.halloween

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.event.lobby.waypoints.EventWaypoint
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HalloweenBasketWaypoints {

    private val config get() = SkyHanniMod.feature.event.lobbyWaypoints.halloweenBasket
    private var isActive = false
    private var isNavigating = false

    private var basketList = setOf<EventWaypoint>()
    private var closestBasket: EventWaypoint? = null

    private val patternGroup = RepoPattern.group("event.lobby-waypoints")

    // TODO add regex tests
    private val scoreboardTitlePattern by patternGroup.pattern(
        "main.scoreboard.title",
        "^HYPIXEL$",
    )
    private val halloweenEventPattern by patternGroup.pattern(
        "main.scoreboard.halloween",
        "^§6Halloween \\d+$",
    )
    private val scoreboardBasketPattern by patternGroup.pattern(
        "main.scoreboard.basket",
        "^Baskets Found: §a\\d+§f/§a\\d+\$",
    )

    @Suppress("MaxLineLength")
    private val basketPattern by patternGroup.pattern(
        "basket",
        "^(?:(?:§.)+You found a Candy Basket! (?:(?:§.)+\\((?:§.)+(?<current>\\d+)(?:§.)+/(?:§.)+(?<max>\\d+)(?:§.)+\\))?|(?:§.)+You already found this Candy Basket!)\$",
    )
    private val basketAllFoundPattern by patternGroup.pattern(
        "basket.all-found",
        "^§a§lCongratulations! You found all Candy Baskets!$",
    )

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(2)) return
        if (!isActive || !isEnabled()) return

        val newClosest = getClosest()
        if (newClosest == closestBasket) {
            newClosest?.let {
                if (!isNavigating && it.position.distanceToPlayer() > 5) {
                    startPathfinding()
                }
            }
            return
        }

        closestBasket = newClosest
        if (config.pathfind.get() && config.enabled.get()) startPathfinding()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.enabled.get()) return
        if (!isActive) return
        if (!isEnabled()) return

        if (basketAllFoundPattern.matches(event.message)) {
            disableFeature()
            return
        }

        if (!basketPattern.matches(event.message)) return
        basketList.minByOrNull { it.position.distanceSqToPlayer() }?.isFound = true
        if (basketList.all { it.isFound }) {
            disableFeature()
            return
        }
        closestBasket = getClosest()
        startPathfinding()
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!isActive) return
        if (!config.enabled.get()) return

        if (config.onlyClosest) {
            closestBasket.render(event)
        } else {
            basketList.forEach {
                it.render(event)
            }
        }
    }

    private fun EventWaypoint?.render(event: SkyHanniRenderWorldEvent) {
        if (this == null) return
        event.drawDynamicText(position, "§dBasket", 1.0)
    }

    @HandleEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        if (SkyBlockUtils.inSkyBlock) {
            isActive = false
            return
        }
        var inHub = false
        var halloweenMatches = false
        var basketMatches = false

        if (scoreboardTitlePattern.matches(ScoreboardData.objectiveTitle.removeColor())) {
            inHub = true
        }
        event.new.forEach {
            if (halloweenEventPattern.matches(it)) {
                halloweenMatches = true
            } else if (scoreboardBasketPattern.matches(it)) {
                basketMatches = true
            }
        }

        val newIsActive = inHub && halloweenMatches && basketMatches
        if (isActive != newIsActive && newIsActive) {
            if (IslandGraphs.currentIslandGraph == null) {
                IslandGraphs.loadLobby("MAIN_LOBBY")
            }
            IslandGraphs.currentIslandGraph?.let {
                val halloweenNodes = it.getNodesWithTags(GraphNodeTag.HALLOWEEN_BASKET)
                basketList = halloweenNodes.map { node ->
                    EventWaypoint(position = node.position, isFound = false)
                }.toSet()
                closestBasket = getClosest(halloweenNodes)
                if (config.pathfind.get() && config.enabled.get()) startPathfinding()
            }
        }
        isActive = newIsActive && basketList.isNotEmpty()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.enabled, config.pathfind) {
            if (config.pathfind.get() && isActive && isEnabled()) startPathfinding()
        }
    }

    private fun startPathfinding() {
        val basket = closestBasket ?: return
        IslandGraphs.pathFind(
            basket.position,
            "§dNext Basket",
            LorenzColor.LIGHT_PURPLE.toColor(),
            onFound = { isNavigating = false },
            condition = { config.pathfind.get() && closestBasket != null && config.enabled.get() },
        )
        isNavigating = true
    }

    private fun getClosest(nodeList: List<GraphNode>? = null): EventWaypoint? {
        val nodes = nodeList ?: IslandGraphs.currentIslandGraph?.getNodesWithTags(GraphNodeTag.HALLOWEEN_BASKET).orEmpty()

        val unFoundBaskets = basketList.filter { !it.isFound }.map { it.position }
        val unFoundNodes = nodes.filter { it.position in unFoundBaskets }
        val currentNode = IslandGraphs.closestNode ?: return null

        val closestNode = unFoundNodes.minByOrNull { GraphUtils.findShortestDistance(currentNode, it) } ?: return null
        return EventWaypoint(position = closestNode.position, isFound = false)
    }

    private fun disableFeature() {
        ChatUtils.chat("Disabling Halloween Basket waypoints since you found all of them!")
        config.enabled.set(false)
    }

    private fun isEnabled() = HypixelData.hypixelLive && !SkyBlockUtils.inSkyBlock

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(13, "event.halloweenBasket", "event.lobbyWaypoints.halloweenBasket")
        event.move(108, "event.lobbyWaypoints.halloweenBasket.allWaypoints", "event.lobbyWaypoints.halloweenBasket.enabled")
    }
}
