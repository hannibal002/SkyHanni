package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.inventory.NpcTradeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LoreCostUtils.LoreCostEntry
import at.hannibal2.skyhanni.utils.LoreCostUtils.hasTradeLine
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * What the items in the currently open npc trade menu cost, and what was bought from it.
 *
 * Reading this once per menu update keeps every feature that needs the costs from parsing the
 * same lore again.
 */
@SkyHanniModule
object NpcTradeApi {

    /**
     * [name] is the display name without color, the form the purchase message uses.
     * [costs] is never empty, an item without readable costs is not a trade.
     */
    class NpcTrade(val internalName: NeuInternalName, val name: String, val costs: List<LoreCostEntry>)

    /**
     * Keyed by [NpcTrade.name].
     *
     * Kept after the menu closed: some shops ask for the amount in a follow up menu, the purchase
     * message then arrives when the shop menu is already gone.
     */
    var trades: Map<String, NpcTrade> = emptyMap()
        private set

    /**
     * REGEX-TEST: You bought Mechamind Chip!
     * REGEX-TEST: You bought Supreme Chocolate Bar x5!
     */
    private val itemBoughtPattern by RepoPattern.pattern(
        "data.npctrade.bought",
        "You bought (?<name>.+?)(?: x(?<amount>[\\d,]+))?!",
    )

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        readTrades(event.inventoryItems.values)
    }

    @HandleEvent
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        readTrades(event.inventoryItems.values)
    }

    // a menu without trades leaves the previous ones alone, they are still needed after it closed
    private fun readTrades(items: Collection<SafeItemStack>) {
        // the same item can sit in several slots, the costs are the same in all of them
        val newTrades = items.mapNotNull { readTrade(it) }.associateBy { it.name }
        if (newTrades.isEmpty()) return
        trades = newTrades
    }

    private fun readTrade(item: SafeItemStack): NpcTrade? {
        val lore = item.getLoreComponent().map { it.formattedTextCompatLessResets() }
        if (!lore.hasTradeLine()) return null

        val name = item.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val costs = lore.readLoreCosts(name).takeIf { it.isNotEmpty() } ?: return null
        return NpcTrade(item.getInternalName(), name.removeColor(), costs)
    }

    /** The form [trades] is keyed by. */
    fun SafeItemStack.tradeName(): String = hoverName.formattedTextCompatLeadingWhiteLessResets().removeColor()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        itemBoughtPattern.matchMatcher(event.message.removeColor()) {
            val trade = trades[group("name")] ?: return
            NpcTradeEvent(trade.internalName, groupOrNull("amount")?.formatLong() ?: 1, trade.costs).post()
        }
    }
}
