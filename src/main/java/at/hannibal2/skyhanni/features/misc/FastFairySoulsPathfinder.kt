package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FastFairySoulsPathfinder {
    val config get() = SkyHanniMod.feature.misc

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
        var disabled = false

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
            localFoundSouls().add(nearest)
        }

        fun pathToNext() {
            if (disabled) return
            if (route.isEmpty()) {
                val message = "§e[SkyHanni] Found all §5$found Fairy Souls §ein ${LorenzUtils.skyBlockIsland.displayName}!"
                IslandGraphs.overrideChatMessage(message)
                allFound()
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

        fun allFound() {
            disabled = true
            localFoundSouls().addAll(route)
        }

        private fun isDataEnabled() = data?.let { !it.disabled } ?: false
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        data = null
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

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
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


            if (island.isInIsland()) {
                data?.checkHaveAll(have)
            } else {
                totalFound[island] = have
                ChatUtils.chat("set ${island.name} to $have")
            }
        }
    }

    private fun Data.checkHaveAll(have: Int): Boolean {
        val haveAll = have > 0 && have == total
        if (haveAll) {
            ChatUtils.debug("found all souls according to hypixel")
            allFound()
        }
        return haveAll
    }

    private fun createEmptyData(): Data = Data(0, 0, mutableListOf(), emptySet()).apply { disabled = true }
    private val calculatingMessageId = ChatUtils.getUniqueMessageId()

    private var calculating = false
    private var calculatingStart = SimpleTimeMark.farPast()

    private fun reload() {
        val graph = IslandGraphs.currentIslandGraph ?: run {
            ChatUtils.debug("island graph is empty")
            data = createEmptyData()
            return
        }
        val foundSouls = localFoundSouls()
        val allSouls = getTargetNodes(graph.nodes)
        val missingSouls = allSouls.filter { it.position !in foundSouls }
        if (missingSouls.isEmpty()) {
            ChatUtils.debug("missingSouls is empty")
            data = createEmptyData()
            return
        }

        data = createEmptyData()
        calculating = true
        calculatingStart = SimpleTimeMark.now()
        "§e[SkyHanni] Calculating Fairy Soul route §b0s".asComponent().send(calculatingMessageId)

        SkyHanniMod.launchCoroutine {
            val route = NavigationUtils.getRoute(missingSouls, maxIterations = 300, neighborhoodSize = 50).toMutableList()
            val duration = calculatingStart.passedSince()
            "§e[SkyHanni] Calculated Fairy Soul route in §b${duration.format(showMilliSeconds = true)}".asComponent()
                .send(calculatingMessageId)
            calculating = false
            setData(foundSouls, allSouls, route)
        }
    }

    private fun setData(
        foundSouls: MutableSet<LorenzVec>,
        allSouls: List<GraphNode>,
        route: MutableList<LorenzVec>,
    ): Data = Data(
        found = foundSouls.size,
        total = allSouls.size,
        route,
        allSouls = allSouls.map { it.position }.toSet(),
    ).also {
        it.pathToNext()
        this.data = it
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        if (duplicatePattern.matches(event.message) || newPattern.matches(event.message)) {
            data?.foundNearby()
        }
    }

    @HandleEvent
    fun onIslandGraphReload(event: IslandGraphReloadEvent) {
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
                add("found: $found")
                add("total: $total")
                add("route: ${route.size}")
                add("foundButNotClickedSoul: $foundButNotClickedSoul")
                add(": $")
            } ?: run {
                add("data is null")
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsoulreset") {
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
        localFoundSouls().clear()
        totalFound[LorenzUtils.skyBlockIsland] = 0
        ChatUtils.chat("Reset found Fairy Souls on ${LorenzUtils.skyBlockIsland.displayName}.")
        reload()
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        ChatUtils.chat("Mark all Fairy Souls as found on ${LorenzUtils.skyBlockIsland.displayName}.")
        data?.allFound()
        reload()
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

    private fun localFoundSouls(): MutableSet<LorenzVec> = foundSouls.getOrPut(LorenzUtils.skyBlockIsland) { mutableSetOf() }

    private fun totalFoundSouls(): Int = totalFound[LorenzUtils.skyBlockIsland] ?: 0

    private fun getTargetNodes(nodes: List<GraphNode>): List<GraphNode> = nodes.filter { it.hasTag(GraphNodeTag.FAIRY_SOUL) }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.fastFairySouls
}
