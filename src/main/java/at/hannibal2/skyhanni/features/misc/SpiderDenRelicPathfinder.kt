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
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationFeedback
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
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
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SpiderDenRelicPathfinder {

    private val config get() = SkyHanniMod.feature.misc

    private var data: Data? = null

    private val relicPathFindConfig = CoroutineSettings("spider relic pathfind")
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
        var disabled = total > 0 && found == total
        var debugState: String? = null

        fun foundNearby() {
            if (disabled) return
            foundButNotClickedRelic = null
            val nearest = getNearestRelic() ?: return
            markFound(nearest)
            pathToNext()
        }

        private fun getNearestRelic(): LorenzVec? {
            val playerLocation = LocationUtils.playerLocation()
            return allRelics.filter { it.distanceToPlayer() < 10 }.minByOrNull { it.distanceSq(playerLocation) }
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
                allFound("found last relic in Spider's Den")
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
                condition = { config.spiderRelicPathfinder && !disabled },
            )
        }

        fun allFound(state: String) {
            disabled = true
            foundRelicsStore().addAll(route)
            found = foundRelicsStore().size
            debugState = state
        }

        fun checkHaveAll(): Boolean {
            val haveAll = total > 0 && foundRelicsStore().size == total
            if (haveAll) allFound("already found all relics in Spider's Den")
            return haveAll
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        data = null
    }

    @HandleEvent(onlyOnIsland = IslandType.SPIDER_DEN)
    fun onIslandGraphReload() {
        if (config.spiderRelicPathfinder) reload()
        else data = null
    }

    @HandleEvent(onlyOnIsland = IslandType.SPIDER_DEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.spiderRelicPathfinder) return
        if (event.isMod(5) && calculating) {
            val duration = calculatingStart.passedSince().format(showMilliSeconds = true)
            "§e[SkyHanni] Calculating Relic route §b$duration".asComponent().send(calculatingMessageId)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.SPIDER_DEN)
    fun onSecondPassed() {
        if (!config.spiderRelicPathfinder) return
        data?.let {
            it.checkNextRelic()
            return
        }
        reload()
    }

    @HandleEvent(onlyOnIsland = IslandType.SPIDER_DEN)
    fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (!config.spiderRelicPathfinder) return
        if (foundPattern.matches(event.chatComponent) || duplicatePattern.matches(event.chatComponent)) {
            data?.foundNearby()
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
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Spider Den Relic Pathfinder")
        if (!IslandType.SPIDER_DEN.isInIsland()) {
            event.addIrrelevant("not on spider island")
            return
        }
        if (!config.spiderRelicPathfinder) {
            event.addIrrelevant("disabled")
            return
        }
        event.addData {
            data?.apply {
                debugState?.let {
                    add(it)
                    add("")
                }
                add("found: $found")
                add("total: $total")
                add("route remaining: ${route.size}")
                add("foundButNotClickedRelic: $foundButNotClickedRelic")
            } ?: add("data is null")
        }
    }

    private val calculatingMessageId = ChatUtils.getUniqueMessageId()
    private var calculating = false
    private var calculatingStart = SimpleTimeMark.farPast()

    private fun reload() {
        val graph = IslandGraphs.currentIslandGraph ?: run {
            data = createEmptyData("island graph is empty")
            return
        }

        val foundRelics = foundRelicsStore()
        val allRelics = getRelicNodes(graph)
        val missingRelics = allRelics.filter { it.position !in foundRelics }

        if (missingRelics.isEmpty()) {
            data = if (foundRelics.isEmpty()) {
                createEmptyData("No relics found in Spider's Den graph")
            } else {
                val size = foundRelics.size
                Data(size, size, mutableListOf(), foundRelics).also {
                    it.debugState = "found all relics in Spider's Den"
                }
            }
            return
        }

        data = Data(0, allRelics.size, mutableListOf(), emptySet())
        if (data?.checkHaveAll() == true) return

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

    private fun createEmptyData(reason: String) =
        Data(0, 0, mutableListOf(), emptySet()).apply {
            disabled = true
            debugState = reason
        }

    private fun getRelicNodes(nodes: List<GraphNode>): List<GraphNode> =
        nodes.filter { it.hasTag(GraphNodeTag.SPIDER_RELIC) }

    private fun foundRelicsStore(): MutableSet<LorenzVec> =
        ProfileStorageData.profileSpecific?.spider?.relics?.found ?: mutableSetOf()

    private fun isDisabledCommand(): Boolean {
        if (config.spiderRelicPathfinder) return false
        ChatUtils.clickableChat(
            "§cSpider Relic Pathfinder disabled. Click to enable!",
            onClick = { config.spiderRelicPathfinder = true },
        )
        return true
    }

    private fun isWrongIslandCommand(): Boolean {
        if (IslandType.SPIDER_DEN.isInIsland()) return false
        ChatUtils.clickableChat(
            "§cNot on Spider's Den. Click to warp!",
            onClick = { HypixelCommands.warp("spider") },
        )
        return true
    }

    private fun onResetCommand() {
        if (isDisabledCommand()) return
        if (isWrongIslandCommand()) return
        foundRelicsStore().clear()
        data = null
        reload()
        ChatUtils.chat("Reset found Relics in Spider's Den.")
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        if (isWrongIslandCommand()) return
        data?.allFound("manually marked all relics as found via command")
        reload()
        ChatUtils.chat("Marked all Relics as found in Spider's Den.")
    }

    private fun onReloadCommand() {
        if (isDisabledCommand()) return
        if (isWrongIslandCommand()) return
        data = null
        reload()
        ChatUtils.chat("Reloaded Relic pathfinder.")
    }
}
