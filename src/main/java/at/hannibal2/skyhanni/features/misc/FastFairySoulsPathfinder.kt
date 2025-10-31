package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.hanni.data.model.GraphNode
import at.hannibal2.hanni.data.model.GraphNodeTag
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.IslandGraphReloadEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.events.minecraft.WorldChangeEvent
import at.hannibal2.hanni.features.misc.pathfind.NavigationFeedback
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.PlayerUtils
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import at.hannibal2.hanni.utils.chat.TextHelper.send
import at.hannibal2.hanni.utils.navigation.NavigationUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@Suppress("MemberVisibilityCanBePrivate")
@HanniModule
object FastFairySoulsPathfinder {
    val config get() = HanniMod.feature.misc

    // TODO this does not work with glacite tunnels, should prob use strings and add the same workaround we have for graph area
    // TODO also once this is fixed, add a chat message when finding the last soul in dwarven mines and have not yet found the souls in glacite tunnels
    private val foundSouls get() = ProfileStorageData.profileSpecific?.fairySouls?.found ?: mutableMapOf()
    private val totalFound get() = ProfileStorageData.profileSpecific?.fairySouls?.totalFound ?: mutableMapOf()

    private var data: Data? = null

    private val patternGroup = RepoPattern.group("misc.fairy-souls")

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

    /**
     * REGEX-TEST: §7Fairy Souls: §e11§7/§d11
     */
    private val loreSoulPattern by patternGroup.pattern(
        "new",
        "§7Fairy Souls: §e(?<have>.*)§7\\/§d(?<total>.*)",
    )

    private class Data(
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
            val nearest = getNearestSoul() ?: return
            found(nearest)
            pathToNext()
        }

        private fun getNearestSoul(): LorenzVec? {
            val playerLocation = LocationUtils.playerLocation()
            val nearest = allSouls.minBy { it.distanceSq(playerLocation) }
            if (nearest.distanceToPlayer() < 10) return nearest

            val inAir = PlayerUtils.inAir()
            if (inAir) {
                val abovePlayer = playerLocation.up(10)
                val aboveNearest = allSouls.minBy { it.distanceSq(abovePlayer) }
                if (aboveNearest.distance(abovePlayer) < 10) return aboveNearest
            }

            IslandGraphs.reportLocation(
                playerLocation,
                userFacingReason = "unknown fairy soul",
                technicalInfo = "user clicked a fairy soul while far away from known fairy souls",
                "nearest soul" to nearest,
                "distance" to nearest.distanceToPlayer().roundTo(1),
                "inAir" to inAir,
            )
            return null
        }

        private fun found(nearest: LorenzVec) {
            if (route.remove(nearest)) {
                found++
            }
            foundSoulsOnCurrentIsland().add(nearest)
        }

        fun pathToNext() {
            if (disabled) return
            if (route.isEmpty()) {
                val message = "§e[Hanni] Found all §5$found Fairy Souls §ein ${SkyBlockUtils.currentIsland.displayName}!"
                NavigationFeedback.sendPathFindMessage(message)
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
            foundSoulsOnCurrentIsland().addAll(route)
            found = foundSoulsOnCurrentIsland().size
            totalFound[SkyBlockUtils.currentIsland] = found
            debugState = state
        }

        fun checkHaveAll(): Boolean {
            val haveAll = total > 0 && amountFoundOnCurrentIsland() == total
            if (haveAll) {
                allFound("already found all souls on ${SkyBlockUtils.currentIsland} according to hypixel data")
            }
            return haveAll
        }

        private fun isDataEnabled() = data?.let { !it.disabled } ?: false
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        data = null
    }

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5)) {
            if (calculating) {
                val duration = calculatingStart.passedSince().format(showMilliSeconds = true)
                "§e[Hanni] Calculating Fairy Soul route §b$duration".asComponent().send(calculatingMessageId)
            }
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled()) return

        data?.let {
            it.checkNextSoul()
            return
        }

        reload()
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Fairy Souls Guide") return

        for (stack in event.inventoryItems.values) {
            val island = IslandType.getByNameOrNull(stack.displayName.removeColor()) ?: continue
            val have = stack.getLore().firstOrNull()?.let {
                loreSoulPattern.matchMatcher(it) {
                    group("have").toIntOrNull()
                }
            } ?: continue

            if (island.isCurrent()) {
                data?.checkHaveAll()
            }
            totalFound[island] = have
        }
    }

    private fun createEmptyData(): Data = Data(0, 0, mutableListOf(), emptySet()).apply { disabled = true }
    private val calculatingMessageId = ChatUtils.getUniqueMessageId()

    private var calculating = false
    private var calculatingStart = SimpleTimeMark.farPast()

    private fun reload() {
        val currentIsland = SkyBlockUtils.currentIsland
        val graph = IslandGraphs.currentIslandGraph ?: run {
            data = createEmptyData().also {
                it.debugState = "island graph is empty"
            }
            return
        }
        val foundSouls = foundSoulsOnCurrentIsland()
        val allSouls = getTargetNodes(graph)
        val missingSouls = allSouls.filter { it.position !in foundSouls }

        if (missingSouls.isEmpty()) {
            data = if (foundSouls.isEmpty()) {
                createEmptyData().also {
                    it.debugState = "There are no fairy souls in the graph network of ${SkyBlockUtils.currentIsland}"
                }
            } else {
                val size = foundSouls.size
                Data(found = size, total = size, route = emptyList<LorenzVec>().toMutableList(), allSouls = foundSouls).also {
                    it.debugState = "found all souls on ${SkyBlockUtils.currentIsland}"
                }
            }
            return
        }

        data = Data(found = 0, total = allSouls.size, route = mutableListOf(), allSouls = emptySet())
        if (data?.checkHaveAll() == true) return
        calculating = true
        calculatingStart = SimpleTimeMark.now()
        "§e[Hanni] Calculating Fairy Soul route §b0s".asComponent().send(calculatingMessageId)

        HanniMod.launchCoroutine("fairy souls pathfind") {
            val route = NavigationUtils.getRoute(missingSouls, maxIterations = 300, neighborhoodSize = 50).toMutableList()
            val duration = calculatingStart.passedSince()
            "§e[Hanni] Calculated Fairy Soul route in §b${duration.format(showMilliSeconds = true)}".asComponent()
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
        this.data = Data(
            found = foundSouls.size,
            total = allSouls.size,
            route,
            allSouls = allSouls.map { it.position }.toSet(),
        ).also {
            it.pathToNext()
        }
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        if (duplicatePattern.matches(event.message) || newPattern.matches(event.message)) {
            data?.foundNearby()
        }
    }

    @HandleEvent(IslandGraphReloadEvent::class)
    fun onIslandGraphReload() {
        if (isEnabled()) {
            reload()
        } else {
            data = null
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Fairy Souls Pathfinder")

        if (!isEnabled()) {
            event.addIrrelevant("disabled")
            return
        }

        event.addData {
            data?.apply {
                debugState?.let {
                    add(it)
                    add("")
                }
                add("found with known location: $found")
                add("actual amount of found souls: ${amountFoundOnCurrentIsland()}")
                add("total: $total")
                add("route: ${route.size}")
                add("foundButNotClickedSoul: $foundButNotClickedSoul")
            } ?: run {
                add("data is null")
            }
            add("")
            add("Amount of Souls found on Islands:")
            for ((island, amount) in totalFound) {
                add("  ${island.displayName}: $amount")
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
            category = CommandCategory.DEVELOPER_TEST
            callback { onReloadPathCommand() }
        }
    }

    private fun onResetCommand() {
        if (isDisabledCommand()) return
        resetFoundOnCurrentIsland()
        reload()
        ChatUtils.chat("Reset found Fairy Souls on ${SkyBlockUtils.currentIsland.displayName}.")
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        val island = SkyBlockUtils.currentIsland
        data?.allFound("manually set all souls in $island as found via command")
        reload()
        ChatUtils.chat("Marked all Fairy Souls as found on ${island.displayName}.")
    }

    private fun onReloadPathCommand() {
        if (isDisabledCommand()) return
        data = null
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

    fun resetFoundOnCurrentIsland() = resetFoundOnIsland(SkyBlockUtils.currentIsland)
    fun resetFoundOnIsland(island: IslandType) {
        totalFound[island] = 0
        foundSouls[island]?.clear()
    }

    fun amountFoundOnCurrentIsland(): Int = amountFoundOnIsland(SkyBlockUtils.currentIsland)
    fun amountFoundOnIsland(island: IslandType): Int = totalFound.getOrDefault(island, 0)

    fun foundSoulsOnCurrentIsland(): MutableSet<LorenzVec> = foundSoulsOnIsland(SkyBlockUtils.currentIsland)
    fun foundSoulsOnIsland(island: IslandType): MutableSet<LorenzVec> = foundSouls.getOrPut(island) { mutableSetOf() }

    private fun getTargetNodes(nodes: List<GraphNode>): List<GraphNode> = nodes.filter { it.hasTag(GraphNodeTag.FAIRY_SOUL) }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.fastFairySouls
}
