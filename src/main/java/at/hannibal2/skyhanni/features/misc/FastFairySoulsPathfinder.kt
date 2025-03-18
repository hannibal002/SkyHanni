package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils

@SkyHanniModule
object FastFairySoulsPathfinder {
    val config get() = SkyHanniMod.feature.misc

    // TODO store in profile storage
    private val foundSouls = mutableMapOf<IslandType, MutableList<LorenzVec>>()

    private var data: Data? = null

    class Data(
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
                disabled = true
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

        private fun isDataEnabled() = data?.let { !it.disabled } ?: false
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        data = null
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

    private fun reload() {
        data = setData().apply {
            pathToNext()
        }
    }

    private fun createEmptyData(): Data = Data(0, 0, mutableListOf(), emptySet()).apply { disabled = true }

    private fun setData(): Data {
        val graph = IslandGraphs.currentIslandGraph ?: run {
            return createEmptyData()
        }
        val foundSouls = localFoundSouls()
        val allSouls = getTargetNodes(graph.nodes)
        val missingSouls = allSouls.filter { it.position !in foundSouls }
        if (missingSouls.isEmpty()) return createEmptyData()

        val route = NavigationUtils.getRoute(missingSouls, maxIterations = 300, neighborhoodSize = 50).toMutableList()

        return Data(
            found = foundSouls.size,
            total = allSouls.size,
            route,
            allSouls = allSouls.map { it.position }.toSet(),
        )
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        // TODO use repo
        if (event.message == "§dYou have already found that Fairy Soul!") {
            data?.foundNearby()
        }
        if (event.message == "§d§lSOUL! §fYou found a §r§dFairy Soul§r§f!") {
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
        event.register("shsoulsdisable") {
            description = "Disable Fairy Souls for the current island."
            category = CommandCategory.USERS_RESET
            callback { onDisableCommand() }
        }
    }

    private fun onResetCommand() {
        if (isDisabledCommand()) return
        localFoundSouls().clear()
        ChatUtils.chat("Reset found Fairy Souls on ${LorenzUtils.skyBlockIsland.displayName}.")
        reload()
    }

    private fun onFoundAllCommand() {
        if (isDisabledCommand()) return
        ChatUtils.chat("Mark all Fairy Souls as found on ${LorenzUtils.skyBlockIsland.displayName}.")
        data?.apply {
            localFoundSouls().addAll(route)
        }
        reload()
    }

    private fun onDisableCommand() {
        if (isDisabledCommand()) return
        ChatUtils.chat("Disabled Fairy Soul Finder for the current island.")
        data = createEmptyData()
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

    private fun localFoundSouls(): MutableList<LorenzVec> = foundSouls.getOrPut(LorenzUtils.skyBlockIsland) { mutableListOf() }

    private fun getTargetNodes(nodes: List<GraphNode>): List<GraphNode> = nodes.filter { it.hasTag(GraphNodeTag.FAIRY_SOUL) }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.fastFairySouls
}
