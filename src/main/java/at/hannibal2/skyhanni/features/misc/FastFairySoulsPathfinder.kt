package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.FairySoulsApi
import at.hannibal2.skyhanni.data.FairySoulsApi.currentIslandData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
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
        var found: Int,
        val total: Int,
        val route: MutableList<LorenzVec>,
        val allSouls: Set<LorenzVec>,
        var foundButNotClickedSoul: LorenzVec? = null,
    ) {
        var disabled = total > 0 && found == total
        var debugState: String? = null

        fun foundNearby() {
            if (disabled) return
            foundButNotClickedSoul = null
            val nearest = allSouls.minBy { it.distanceSqToPlayer() }
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
            found(nearest)
            pathToNext()
        }

        private fun found(nearest: LorenzVec) {
            if (route.remove(nearest)) {
                found++
            }
            currentIslandData.add(nearest)
        }

        fun pathToNext() {
            if (disabled) return
            if (route.isEmpty()) {
                val message = "§e[SkyHanni] Found all §5$found Fairy Souls §ein ${SkyBlockUtils.currentIsland.displayName}!"
                IslandGraphs.overrideChatMessage(message)
                allFound("found last soul of ${SkyBlockUtils.currentIsland}")
            } else {
                pathTo(route.first())
            }
        }

        fun checkNextSoul() {
            if (disabled) return
            val lastSoul = foundButNotClickedSoul ?: return

            if (lastSoul.distanceToPlayer() > 5) {
                pathTo(lastSoul)
                foundButNotClickedSoul = null
            }
        }

        private fun pathTo(loc: LorenzVec) {
            val percentage = (found.toDouble() / total) * 100
            val percentageLabel = "§8(§b${percentage.roundTo(1)}%§8)"
            IslandGraphs.pathFind(
                loc,
                "§b$found/$total §5Fairy Souls $percentageLabel",
                LorenzColor.DARK_PURPLE.toColor(),
                onFound = {
                    foundButNotClickedSoul = loc
                },
                condition = { isEnabled() && isDataEnabled() },
            )
        }

        fun allFound(state: String) {
            disabled = true
            currentIslandData.addAll(route)
            found = currentIslandData.amountFound
            debugState = state
        }

        fun checkHaveAll(): Boolean {
            val haveAll = total > 0 && currentIslandData.amountFound == total
            if (haveAll) {
                allFound("already found all souls on ${SkyBlockUtils.currentIsland} according to hypixel data")
            }
            return haveAll
        }

        private fun isDataEnabled() = islandData?.let { !it.disabled } ?: false
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
            if (calculating) {
                val duration = calculatingStart.passedSince().format(showMilliSeconds = true)
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

    private fun createEmptyData(): IslandData = IslandData(0, 0, mutableListOf(), emptySet()).apply { disabled = true }
    private val calculatingMessageId = ChatUtils.getUniqueMessageId()

    private var calculating = false
    private var calculatingStart = SimpleTimeMark.farPast()

    private fun reload() {
        val currentIsland = SkyBlockUtils.currentIsland
        val graph = IslandGraphs.currentIslandGraph ?: run {
            islandData = createEmptyData().also {
                it.debugState = "island graph is empty"
            }
            return
        }
        val foundSouls = currentIslandData.foundSouls
        val allSouls = getTargetNodes(graph.nodes)
        val missingSouls = allSouls.filter { it.position !in foundSouls }

        if (missingSouls.isEmpty()) {
            islandData = if (foundSouls.isEmpty()) {
                createEmptyData().also {
                    it.debugState = "There are no fairy souls in the graph network of ${SkyBlockUtils.currentIsland}"
                }
            } else {
                val size = foundSouls.size
                IslandData(found = size, total = size, route = emptyList<LorenzVec>().toMutableList(), allSouls = foundSouls).also {
                    it.debugState = "found all souls on ${SkyBlockUtils.currentIsland}"
                }
            }
            return
        }

        islandData = IslandData(found = 0, total = allSouls.size, route = mutableListOf(), allSouls = emptySet())
        if (islandData?.checkHaveAll() == true) return
        calculating = true
        calculatingStart = SimpleTimeMark.now()
        "§e[SkyHanni] Calculating Fairy Soul route §b0s".asComponent().send(calculatingMessageId)

        SkyHanniMod.launchCoroutine {
            val route = NavigationUtils.getRoute(missingSouls, maxIterations = 300, neighborhoodSize = 50).toMutableList()
            val duration = calculatingStart.passedSince()
            "§e[SkyHanni] Calculated Fairy Soul route in §b${duration.format(showMilliSeconds = true)}".asComponent()
                .send(calculatingMessageId)
            calculating = false
            // TODO: fix the root issue of all changes being ignored while calculating (e.g. island change without this check or commands)
            if (currentIsland == SkyBlockUtils.currentIsland) { // Only set data if the island has not changed during calculation
                setData(foundSouls, allSouls, route)
            }
        }
    }

    private fun setData(
        foundSouls: MutableSet<LorenzVec>,
        allSouls: List<GraphNode>,
        route: MutableList<LorenzVec>,
    ) {
        islandData = IslandData(
            found = foundSouls.size,
            total = allSouls.size,
            route,
            allSouls = allSouls.map { it.position }.toSet(),
        ).also { it.pathToNext() }
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
                add("found with known location: $found")
                add("actual amount of found souls: ${currentIslandData.amountFound}")
                add("total: $total")
                add("route: ${route.size}")
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
    }

    private fun onResetCommand() {
        if (isDisabledCommand()) return
        currentIslandData.reset()
        ChatUtils.chat("Reset found Fairy Souls on ${SkyBlockUtils.currentIsland.displayName}.")
        reload()
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        val island = SkyBlockUtils.currentIsland
        ChatUtils.chat("Marked all Fairy Souls as found on ${island.displayName}.")
        islandData?.allFound("manually set all souls in $island as found via command")
        reload()
    }

    private fun isDisabledCommand(): Boolean {
        if (isEnabled()) return false
        ChatUtils.clickableChat(
            "§cFairy Souls are disabled. Click to enable!",
            onClick = {
                config.fastFairySouls = true
                reload()
            },
        )
        return true
    }

    private fun getTargetNodes(nodes: List<GraphNode>): List<GraphNode> = nodes.filter { it.hasTag(GraphNodeTag.FAIRY_SOUL) }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fastFairySouls
}
