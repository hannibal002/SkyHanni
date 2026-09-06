package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarOrdersLoadedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

/**
 * Hides redundant lines from the item tooltips inside the bazaar order inventory.
 */
@SkyHanniModule
object BazaarOrderTooltip {

    private val config get() = SkyHanniMod.feature.inventory.bazaar

    private val patternGroup = RepoPattern.group("bazaar.orders.tooltip")

    /**
     * Buy orders name the sellers, sell offers name the buyers, and both switch to a singular
     * header while only one player is involved.
     *
     * REGEX-TEST: Vendors:
     * REGEX-TEST: Single vendor:
     * REGEX-TEST: Customers:
     * REGEX-TEST: Single customer:
     */
    private val tradePartnerHeaderPattern by patternGroup.pattern(
        "tradepartners.colorless",
        "(?:Vendors|Single vendor|Customers|Single customer):",
    )

    private var allOrdersOwn = false

    @HandleEvent
    private fun onBazaarOrdersLoaded(event: BazaarOrdersLoadedEvent) {
        allOrdersOwn = event.orders.all { it.isOwn }
    }

    @HandleEvent
    private fun onInventoryClose() {
        allOrdersOwn = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!BazaarOrderApi.inOrderInventory()) return

        if (config.hideTradePartners) {
            event.toolTip.removeBlock { tradePartnerHeaderPattern.matches(it) }
        }
        // The owner only says something useful while a co-op member has orders in here as well.
        if (config.hideOrderOwner && allOrdersOwn) {
            event.toolTip.removeBlock { BazaarOrderApi.ownerPattern.matches(it) }
        }
    }

    /**
     * Removes the block starting at the line matching [isHeader], up to and including the empty
     * line that follows it. A block at the end of the tooltip has none and is removed to the end.
     */
    private fun MutableList<Component>.removeBlock(isHeader: (Component) -> Boolean) {
        val start = indexOfFirst(isHeader).takeIf { it != -1 } ?: return
        var end = start
        while (end < size && this[end].string.isNotBlank()) end++
        subList(start, (end + 1).coerceAtMost(size)).clear()
    }
}
