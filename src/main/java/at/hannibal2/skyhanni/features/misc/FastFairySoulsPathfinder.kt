package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.FairySoulsApi
import at.hannibal2.skyhanni.data.FairySoulsApi.currentData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
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
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object FastFairySoulsPathfinder {
    val config get() = SkyHanniMod.feature.misc

    // TODO this does not work with glacite tunnels, should prob use strings and add the same workaround we have for graph area
    // TODO also once this is fixed, add a chat message when finding the last soul in dwarven mines and have not yet found the souls in glacite tunnels

    private var islandData: IslandData? = null

    private val patternGroup = FairySoulsApi.patternGroup

    /**
     * REGEX-TEST: §dYou have already found that Fairy Soul!
     */
    private val duplicatePattern by patternGroup.pattern(
        "chat.duplicat",
        "§dYou have already found that Fairy Soul!",
    )

    /**
     * REGEX-TEST: §d§lSOUL! §fYou found a §r§dFairy Soul§r§f!
     */
    private val newPattern by patternGroup.pattern(
        "chat.new",
        "§d§lSOUL! §fYou found a §r§dFairy Soul§r§f!",
    )

    private class IslandData(
        val allSouls: List<GraphNode>,
        val fairySoulsData: FairySoulsApi.IslandFairySoulsData = currentData,
    ) {
        val missingSouls = allSouls.filter { it.position !in fairySoulsData.foundSouls }.toMutableList()

        var calculating = false
        var calculatingStart = SimpleTimeMark.farPast()

        var disabled = false
        var debugState: String? = null
        var foundButNotClickedSoul: LorenzVec? = null

        var route: List<LorenzVec>? = null

        init {
            calculate()
        }

        fun calculate() {
            if (checkHaveAll() || disabled) return

            calculating = true
            calculatingStart = SimpleTimeMark.now()
            "§e[SkyHanni] Calculating Fairy Soul route §b0s".asComponent().send(calculatingMessageId)

            SkyHanniMod.launchCoroutine {
                route = NavigationUtils.getRoute(missingSouls.toList(), maxIterations = 300, neighborhoodSize = 50).toMutableList()
                val duration = calculatingStart.passedSince()
                "§e[SkyHanni] Calculated Fairy Soul route in §b${duration.format(showMilliSeconds = true)}".asComponent()
                    .send(calculatingMessageId)
                calculating = false
                print("Calculated Fairy Soul route in ${duration.format(showMilliSeconds = true)}")
                pathToNext()
            }
        }

        fun foundNearby() {
            if (disabled) return
            foundButNotClickedSoul = null
            val nearest = missingSouls.map { it.position }.minBy { it.distanceSqToPlayer() }
            if (nearest.distanceToPlayer() > 10) {
                ErrorManager.logErrorStateWithData(
                    "unknown fairy soul",
                    "user clicked a fairy soul while far away from known fairy souls",
                    "nearest loc" to nearest,
                    "player loc" to LocationUtils.playerLocation(),
                    "distance" to nearest.distanceToPlayer().roundTo(1),
                )
                return
            }
            fairySoulsData.add(nearest)
            pathToNext()
        }

        fun pathToNext() {
            if (disabled) return
            val route = route ?: return
            if (route.isEmpty()) {
                val message =
                    "§e[SkyHanni] Found all §5${fairySoulsData.amountFound} Fairy Souls §ein ${SkyBlockUtils.currentIsland.displayName}!"
                IslandGraphs.overrideChatMessage(message)
                allFound("found last soul of ${SkyBlockUtils.currentIsland}")
            } else {
                pathTo(route.first())
            }
        }

        fun checkNextSoul() {
            if (disabled || calculating) return
            val lastSoul = foundButNotClickedSoul ?: return

            if (lastSoul.distanceToPlayer() > 5) {
                pathTo(lastSoul)
                foundButNotClickedSoul = null
            }
        }

        private fun pathTo(loc: LorenzVec) {
            val percentage = (fairySoulsData.amountFound.toDouble() / allSouls.size) * 100
            val percentageLabel = "§8(§b${percentage.roundTo(1)}%§8)"
            IslandGraphs.pathFind(
                loc,
                "§b${fairySoulsData.amountFound}/${allSouls.size} §5Fairy Souls $percentageLabel",
                LorenzColor.DARK_PURPLE.toColor(),
                onFound = {
                    foundButNotClickedSoul = loc
                },
                condition = { isEnabled() && isDataEnabled() },
            )
        }

        fun allFound(state: String) {
            disabled = true
            val route = route ?: return
            fairySoulsData.addAll(route)
            debugState = state
        }

        fun checkHaveAll(): Boolean {
            val haveAll = allSouls.isNotEmpty() && fairySoulsData.amountFound == allSouls.size
            if (haveAll) {
                allFound("found all souls on ${SkyBlockUtils.currentIsland}")
            }
            return haveAll
        }

        private fun isDataEnabled() = islandData?.let { !it.disabled } ?: false
    }

    private fun createEmptyData(debug: String): IslandData = IslandData(emptyList()).apply {
        disabled = true
        debugState = debug
    }

    private val calculatingMessageId = ChatUtils.getUniqueMessageId()

    private fun reload() {
        val graph = IslandGraphs.currentIslandGraph ?: run {
            islandData = createEmptyData("island graph is empty")
            return
        }
        val allSouls = getTargetNodes(graph.nodes)

        if (allSouls.isEmpty()) {
            islandData = createEmptyData("There are no fairy souls in the graph network of ${SkyBlockUtils.currentIsland}")
        }

        IslandData(allSouls = allSouls)
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        islandData = null
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        if (duplicatePattern.matches(event.message) || newPattern.matches(event.message)) {
            islandData?.foundNearby()
        }
    }

    @HandleEvent(IslandGraphReloadEvent::class)
    fun onIslandGraphReload() {
        if (isEnabled()) {
            reload()
        } else {
            islandData = null
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5)) {
            val islandData = islandData ?: return
            if (islandData.calculating) {
                val duration = islandData.calculatingStart.passedSince().format(showMilliSeconds = true)
                "§e[SkyHanni] Calculating Fairy Soul route §b$duration".asComponent().send(calculatingMessageId)
            }
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled()) return

        islandData?.let {
            it.checkNextSoul()
            return
        }

        reload()
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        islandData?.checkHaveAll()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Fairy Souls Pathfinder")

        if (!isEnabled()) {
            event.addIrrelevant("disabled")
            return
        }

        event.addData {
            islandData?.apply {
                debugState?.let {
                    add(it)
                    add("")
                }
                add("found with known location: ${currentData.foundSouls.size}")
                add("actual amount of found souls: ${currentData.amountFound}")
                add("total: ${allSouls.size}")
                val route = route
                if (route == null) {
                    add("route: not calculated yet, calculating: ${calculatingStart.passedSince().format(showMilliSeconds = true)}")
                } else if (route.isEmpty()) {
                    add("route: empty")
                } else {
                    add("route: ${route.size}")
                }
                add("foundButNotClickedSoul: $foundButNotClickedSoul")
            } ?: run {
                add("islandData is null")
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsoulsreset") {
            description = "Reset known Fairy Souls for the current island."
            category = CommandCategory.USERS_RESET
            callback { onResetCommand() }
        }
        event.register("shsoulsfoundall") {
            description = "Mark all Fairy Souls for the current island as found."
            category = CommandCategory.USERS_RESET
            callback { onFoundAllCommand() }
        }
        event.register("shsoulsreloadpath") {
            description = "Reload the Fairy Souls pathfinder."
            category = CommandCategory.USERS_RESET
            callback { onReloadPathCommand() }
        }
    }

    private fun onResetCommand() {
        if (isDisabledCommand()) return
        currentData.reset()
        reload()
        ChatUtils.chat("Reset found Fairy Souls on ${SkyBlockUtils.currentIsland.displayName}.")
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        val island = SkyBlockUtils.currentIsland
        islandData?.allFound("manually set all souls in $island as found via command")
        reload()
        ChatUtils.chat("Marked all Fairy Souls as found on ${island.displayName}.")
    }

    private fun onReloadPathCommand() {
        if (isDisabledCommand()) return
        islandData = null
        reload()
        ChatUtils.chat("Reloaded Fairy Souls pathfinder.")
    }

    private fun isDisabledCommand(): Boolean {
        if (isEnabled()) return false
        ChatUtils.clickableChat(
            "§cFairy Souls are disabled. Click to enable!",
            onClick = {
                config.fastFairySouls = true
            },
        )
        return true
    }

    private fun getTargetNodes(nodes: List<GraphNode>): List<GraphNode> = nodes.filter { it.hasTag(GraphNodeTag.FAIRY_SOUL) }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fastFairySouls
}
