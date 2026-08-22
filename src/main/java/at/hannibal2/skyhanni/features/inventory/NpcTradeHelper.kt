package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.CurrencyApi
import at.hannibal2.skyhanni.data.NpcTradeApi
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.InventoryUtils.isTopInventory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.takeUnlessEmpty
import at.hannibal2.skyhanni.utils.LoreCostUtils
import at.hannibal2.skyhanni.utils.LoreCostUtils.LoreCostEntry
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.network.chat.Component

@SkyHanniModule
object NpcTradeHelper {

    private val config get() = SkyHanniMod.feature.inventory.npcTrade

    /** Null when the trades this was built from may have changed since. */
    private var cachedTradeItems: Map<String, TradeItem>? = null

    /**
     * Built on first use instead of in an event, so that NpcTradeApi has certainly read the menu
     * and CurrencyApi has certainly booked a purchase by the time this is read.
     */
    private val tradeItems: Map<String, TradeItem>
        get() = cachedTradeItems ?: buildTradeItems().also { cachedTradeItems = it }

    /**
     * [evaluable] is false when the owned amount is unknown, for example a currency SkyHanni
     * does not track. Such an item is never marked as affordable and shows a question mark
     * instead of an amount.
     */
    private class CostLine(val rawLine: String, val text: String, val covered: Boolean, val evaluable: Boolean) {
        /** The tooltip adds color codes of its own to the lore line, so the lookup ignores them. */
        val cleanLine = rawLine.removeColor()
    }

    private class TradeItem(val lines: List<CostLine>, val headerSuffix: String) {
        val canAfford = lines.all { it.evaluable && it.covered }
    }

    @HandleEvent
    private fun onInventoryFullyOpened() {
        cachedTradeItems = null
    }

    @HandleEvent
    private fun onInventoryUpdated() {
        cachedTradeItems = null
    }

    @HandleEvent
    private fun onInventoryClose() {
        cachedTradeItems = null
    }

    // the purchase changed what the player owns, every affordability mark is stale now
    @HandleEvent
    private fun onNpcTrade() {
        cachedTradeItems = null
    }

    private fun buildTradeItems(): Map<String, TradeItem> =
        NpcTradeApi.trades.mapValues { (_, trade) -> buildTradeItem(trade.costs) }

    private fun buildTradeItem(costs: List<LoreCostEntry>): TradeItem {
        var coinTotal = 0.0
        val otherCosts = mutableListOf<String>()
        val lines = costs.map { entry ->
            val currency = SkyblockCurrency.getByInternalNameOrNull(entry.internalName)
            if (currency != null && currency.coinValue == null) {
                otherCosts.add(currency.formatAmount(entry.amount))
            } else {
                coinTotal += entry.internalName.getPrice() * entry.amount
            }
            buildCostLine(entry, currency)
        }

        val parts = buildList {
            if (coinTotal > 0) add(coinTotal.formatCoin())
            addAll(otherCosts)
        }
        val headerSuffix = if (parts.isEmpty()) "" else " §7(${parts.joinToString(" §7+ ")}§7)"
        return TradeItem(lines, headerSuffix)
    }

    private fun buildCostLine(entry: LoreCostEntry, currency: SkyblockCurrency?): CostLine {
        val internalName = entry.internalName
        val amount = entry.amount

        val owned = when {
            internalName == NeuInternalName.MISSING_ITEM -> null
            currency != null -> currency.getOwnedAmountOrNull()
            // essence is a repo item, but it is not carried in the inventory and cannot be counted
            internalName.isEssence() -> CurrencyApi.getEssenceOrNull(internalName)
            else -> internalName.getAmountInInventory().toLong()
        }
        val covered = owned != null && owned >= amount

        val suffix = when {
            internalName == NeuInternalName.MISSING_ITEM -> "§c!"
            owned == null -> " §8?§7/§8${amount.addSeparators()}"
            covered -> " §a✔"
            else -> " §8${owned.addSeparators()}§7/§8${amount.addSeparators()}"
        }
        val sacks = if (currency == null) internalName.getAmountInSacksOrNull() ?: 0 else 0
        val sackText = if (sacks > 0) " §7(sacks: §a${sacks.addSeparators()}§7)" else ""

        val name = when {
            // the line could not be read, leaving it untouched beats replacing it with a broken name
            internalName == NeuInternalName.MISSING_ITEM -> entry.rawLine
            // currencies other than coins have no price, getPriceName would show a "(0)" behind them
            currency != null && currency.coinValue == null -> currency.formatAmount(amount)
            else -> internalName.getPriceName(amount)
        }

        return CostLine(entry.rawLine, name + suffix + sackText, covered, owned != null)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!config.costBreakdown) return
        val tradeItem = tradeItems[event.itemStack.cleanName] ?: return

        for ((index, component) in event.toolTip.withIndex()) {
            // the tooltip prefixes every lore line, the lines from the item itself do not have it
            val line = component.formattedTextCompatLessResets().removePrefix("§5§o")
            if (LoreCostUtils.isCostHeader(line)) {
                // the inline form writes the only cost entry into the header line itself
                val inlineCost = tradeItem.lines.firstOrNull { line.contains(it.rawLine) }
                val newLine = inlineCost?.let { line.replace(it.rawLine, it.text) } ?: line
                event.toolTip[index] = Component.literal(newLine + tradeItem.headerSuffix)
                continue
            }
            val costLine = tradeItem.lines.firstOrNull { it.cleanLine == line.removeColor() } ?: continue
            event.toolTip[index] = Component.literal(costLine.text)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightAffordable || tradeItems.isEmpty()) return

        for (slot in event.container.slots) {
            // the trades outlive the menu, an item of the same name in the player inventory is not one
            if (!slot.isTopInventory()) continue
            val item = slot.item.takeUnlessEmpty() ?: continue
            if (tradeItems[item.cleanName]?.canAfford == true) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }
}
