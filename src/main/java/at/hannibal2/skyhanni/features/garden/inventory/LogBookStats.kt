package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object LogBookStats {

    private val groupPattern = RepoPattern.group("garden.inventory.logbook")

    /**
     * REGEX-TEST: Times Visited: 22
     */
    private val visitedPattern by groupPattern.pattern(
        "visited.colorless",
        "Times Visited: (?<timesVisited>[0-9,.]+)",
    )

    /**
     * REGEX-TEST: Offers Accepted: 21
     */
    private val acceptedPattern by groupPattern.pattern(
        "accepted.colorless",
        "Offers Accepted: (?<timesAccepted>[0-9,.]+)",
    )

    /**
     * REGEX-TEST: Visitor's Logbook
     * REGEX-TEST: (1/5) Visitor's Logbook
     * REGEX-TEST: (10/11) Visitor's Logbook
     */
    private val inventoryNamePattern by groupPattern.pattern(
        "inventory-name",
        "(?:\\(\\d+/\\d+\\) )?Visitor's Logbook",
    )

    private val config get() = GardenApi.config
    private var display = emptyList<Renderable>()
    private val loggedVisitors = mutableMapOf<String, VisitorInfo>()
    private var inInventory = false

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (IslandType.GARDEN_GUEST.isInIsland()) return

        if (!inventoryNamePattern.matches(event.inventoryName)) return

        inInventory = true

        for ((_, item) in event.inventoryItems) {
            val visitorName = item.hoverName.string.takeIf { it.isNotEmpty() } ?: continue
            var timesVisited = 0L
            var timesAccepted = 0L
            val lore = item.getCleanLore()
            visitedPattern.firstMatcher(lore) {
                timesVisited += group("timesVisited").formatLong()
            }
            acceptedPattern.firstMatcher(lore) {
                timesAccepted += group("timesAccepted").formatLong()
            }

            loggedVisitors[visitorName] = VisitorInfo(timesVisited, timesAccepted)
        }

        display = buildList {
            val visited = loggedVisitors.values.sumOf { it.timesVisited }
            val accepted = loggedVisitors.values.sumOf { it.timesAccepted }
            val visitingNow = VisitorApi.getVisitors().size
            val denied = visited - accepted - visitingNow
            addString("§6Times Visited: §b${visited.addSeparators()}")
            addString("§6Times Accepted: §a${accepted.addSeparators()}")
            addString("§6Times Denied: §c${denied.addSeparators()}")
        }
    }

    @HandleEvent
    private fun onChestGuiRender() {
        if (IslandType.GARDEN_GUEST.isInIsland()) return
        if (inInventory && config.showLogBookStats) {
            config.logBookStatsPos.renderRenderables(
                display,
                extraSpace = 5,
                posLabel = "Visitor's LogBook Stats",
            )
        }
    }

    @HandleEvent
    private fun onProfileJoin() {
        display = emptyList()
        loggedVisitors.clear()
        inInventory = false
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
    }

    private data class VisitorInfo(
        var timesVisited: Long = 0,
        var timesAccepted: Long = 0,
    )
}
