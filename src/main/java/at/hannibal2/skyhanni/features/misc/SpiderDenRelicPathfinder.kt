package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationFeedback
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SpiderDenRelicPathfinder {

    private val config get() = SkyHanniMod.feature.misc

    private var data: Data = createEmptyData(Data.DebugState.NOT_INITIALIZED)

    private val relicPathFindConfig = CoroutineConfig("spider relic pathfind")
    private val patternGroup = RepoPattern.group("misc.spider-relic")

    /**
     * REGEX-TEST: +10,000 Coins! (2/28 Relics)
     */
    private val foundPattern by patternGroup.pattern(
        key = "chat.found",
        fallback = "\\+[\\d,]+ Coins! \\(\\d+/\\d+ Relics\\)",
    )

    /**
     * REGEX-TEST: You've already found this relic!
     */
    private val duplicatePattern by patternGroup.pattern(
        key = "chat.duplicate",
        fallback = "You've already found this relic!|You've already found all the relics!",
    )

    private class Data(
        var found: Int,
        val total: Int,
        val route: MutableList<LorenzVec>,
        val allRelics: Set<LorenzVec>,
        var foundButNotClickedRelic: LorenzVec? = null,
    ) {
        val disabled get() = total > 0 && found == total

        enum class DebugState {
            NOT_INITIALIZED, ISLAND_GRAPH_EMPTY,
            NO_RELICS_IN_GRAPH, ALL_FOUND, ACTIVE
        }
        var debugState: DebugState = DebugState.ACTIVE

        fun foundNearby() {
            if (disabled) return
            foundButNotClickedRelic = null
            val playerLocation = LocationUtils.playerLocation()
            val nearest = allRelics
                .filter { it.distanceToPlayer() < 10 }
                .minByOrNull { it.distanceSq(playerLocation) } ?: return
            markFound(nearest)
            pathToNext()
        }

        private fun markFound(relic: LorenzVec) {
            if (route.remove(relic)) found++
            foundRelicsStore().add(relic)
        }

        fun pathToNext() {
            if (disabled) return
            if (route.isEmpty()) {
                val message = "§e[SkyHanni] Found all §5$found Relics §ein Spider's Den!"
                NavigationFeedback.sendPathFindMessage(message)
                allFound()
            } else {
                pathTo(route.first())
            }
        }

        fun checkNextRelic() {
            if (disabled) return
            val lastRelic = foundButNotClickedRelic ?: return
            if (lastRelic.distanceToPlayer() > 5) {
                pathTo(lastRelic)
                foundButNotClickedRelic = null
            }
        }

        private fun pathTo(loc: LorenzVec) {
            val percentage = (found.toDouble() / total) * 100
            val percentageLabel = "§8(§b${percentage.roundTo(1)}%§8)"
            IslandGraphs.pathFind(
                loc,
                "§b$found/$total §5Relics $percentageLabel",
                LorenzColor.DARK_PURPLE.toColor(),
                onFound = { foundButNotClickedRelic = loc },
                condition = { isEnabled() && !disabled },
            )
        }

        fun allFound() {
            foundRelicsStore().addAll(route)
            found = foundRelicsStore().size
            debugState = DebugState.ALL_FOUND
        }

        private val haveAll get() = total > 0 && foundRelicsStore().size == total

        fun checkHaveAll(): Boolean {
            if (haveAll) allFound()
            return haveAll
        }
    }

    @HandleEvent(WorldChangeEvent::class, onlyOnSkyblock = true, onlyOnIsland = IslandType.SPIDER_DEN)
    fun onWorldChange() {
        data = createEmptyData(Data.DebugState.NOT_INITIALIZED)
    }

    @HandleEvent(IslandGraphReloadEvent::class, onlyOnSkyblock = true, onlyOnIsland = IslandType.SPIDER_DEN)
    fun onIslandGraphReload() {
        if (isEnabled()) reload()
        else data = createEmptyData(Data.DebugState.NOT_INITIALIZED)
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5) && calculating) {
            val duration = calculatingStart.passedSince().format(showMilliSeconds = true)
            "§e[SkyHanni] Calculating Relic route §b$duration".asComponent().send(calculatingMessageId)
        }
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnSkyblock = true, onlyOnIsland = IslandType.SPIDER_DEN)
    fun onSecondPassed() {
        if (!isEnabled()) return
        if (data.disabled) {
            reload()
            return
        }
        data.checkNextRelic()
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (!isEnabled()) return
        if (foundPattern.matches(event.chatComponent) || duplicatePattern.matches(event.chatComponent)) {
            data.foundNearby()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shrelicsreset") {
            description = "Reset known Spider Den Relics."
            category = CommandCategory.USERS_RESET
            simpleCallback { onResetCommand() }
        }
        event.registerBrigadier("shrelicsfoundall") {
            description = "Mark all Spider Den Relics as found."
            category = CommandCategory.USERS_RESET
            simpleCallback { onFoundAllCommand() }
        }
        event.registerBrigadier("shrelicsreload") {
            description = "Reload Spider Den Relic pathfinder."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { onReloadCommand() }
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Spider Den Relic Pathfinder")
        if (!isEnabled()) {
            event.addIrrelevant("disabled")
            return
        }
        event.addData {
            data.apply {
                add(debugState.name)
                add("")
                add("found: $found")
                add("total: $total")
                add("route remaining: ${route.size}")
                add("foundButNotClickedRelic: $foundButNotClickedRelic")
            }
        }
    }

    private val calculatingMessageId = ChatUtils.getUniqueMessageId()
    private var calculating = false
    private var calculatingStart = SimpleTimeMark.farPast()

    private fun reload() {

        val graph = IslandGraphs.currentIslandGraph ?: run {
            data = createEmptyData(Data.DebugState.ISLAND_GRAPH_EMPTY)
            return
        }

        val foundRelics = foundRelicsStore()
        val allRelics = getRelicNodes(graph)
        val missingRelics = allRelics.filter { it.position !in foundRelics }

        if (missingRelics.isEmpty()) {
            data = if (foundRelics.isEmpty()) {
                createEmptyData(Data.DebugState.NO_RELICS_IN_GRAPH)
            } else {
                val size = foundRelics.size
                Data(size, size, mutableListOf(), foundRelics).also {
                    it.debugState = Data.DebugState.ALL_FOUND
                }
            }
            return
        }

        data = Data(0, allRelics.size, mutableListOf(), emptySet())
        if (data.checkHaveAll()) return

        calculating = true
        calculatingStart = SimpleTimeMark.now()
        "§e[SkyHanni] Calculating Relic route §b0s".asComponent().send(calculatingMessageId)

        val currentIsland = SkyBlockUtils.currentIsland
        relicPathFindConfig.launch {
            val route = NavigationUtils.getRoute(
                missingRelics,
                maxIterations = 300,
                neighborhoodSize = 50,
            ).toMutableList()

            val duration = calculatingStart.passedSince().format(showMilliSeconds = true)
            "§e[SkyHanni] Calculated Relic route in §b$duration".asComponent().send(calculatingMessageId)
            calculating = false

            if (currentIsland == SkyBlockUtils.currentIsland) {
                data = Data(
                    found = foundRelics.size,
                    total = allRelics.size,
                    route = route,
                    allRelics = allRelics.map { it.position }.toSet(),
                ).also { it.pathToNext() }
            }
        }
    }

    private fun createEmptyData(state: Data.DebugState) =
        Data(0, 0, mutableListOf(), emptySet()).apply {
            debugState = state
        }

    private fun getRelicNodes(nodes: List<GraphNode>): List<GraphNode> =
        nodes.filter { it.hasTag(GraphNodeTag.SPIDER_RELIC) }

    private fun foundRelicsStore(): MutableSet<LorenzVec> =
        ProfileStorageData.profileSpecific?.spider?.relics?.found ?: mutableSetOf()

    private fun isEnabled() = config.spiderRelicPathfinder

    private fun isDisabledCommand(): Boolean {
        if (isEnabled()) return false
        ChatUtils.clickableChat(
            "§cSpider Relic Pathfinder disabled, or not in Spider's Den. Click to enable!",
            onClick = { config.spiderRelicPathfinder = true },
        )
        return true
    }

    private fun onResetCommand() {
        if (isDisabledCommand()) return
        foundRelicsStore().clear()
        data = createEmptyData(Data.DebugState.NOT_INITIALIZED)
        reload()
        ChatUtils.chat("Reset found Relics in Spider's Den.")
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        data.allFound()
        reload()
        ChatUtils.chat("Marked all Relics as found in Spider's Den.")
    }

    private fun onReloadCommand() {
        if (isDisabledCommand()) return
        data = createEmptyData(Data.DebugState.NOT_INITIALIZED)
        reload()
        ChatUtils.chat("Reloaded Relic pathfinder.")
    }
}
