package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LoreCostUtils
import at.hannibal2.skyhanni.utils.LoreCostUtils.LoreCostEntry
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.network.chat.Component

@SkyHanniModule
object NpcTradeHelper {

    private val config get() = SkyHanniMod.feature.inventory.npcTrade

    private const val TRADE_LINE = "§eClick to trade!"

    private var tradeItems = mapOf<Int, TradeItem>()

    /**
     * [evaluable] is false when the owned amount is unknown, for example a currency SkyHanni
     * does not track. Such an item is never marked as affordable.
     */
    private class CostLine(val rawLine: String, val text: String, val covered: Boolean, val evaluable: Boolean)

    private class TradeItem(val lines: List<CostLine>, val headerSuffix: String) {
        val canAfford = lines.all { it.evaluable && it.covered }
    }

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        updateItems(event.inventoryItems)
    }

    @HandleEvent
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        updateItems(event.inventoryItems)
    }

    @HandleEvent
    private fun onInventoryClose() {
        tradeItems = emptyMap()
    }

    private fun updateItems(inventoryItems: Map<Int, SafeItemStack>) {
        if (!isEnabled()) {
            tradeItems = emptyMap()
            return
        }
        tradeItems = buildMap {
            for ((slot, item) in inventoryItems) {
                readTradeItem(item)?.let { put(slot, it) }
            }
        }
    }

    private fun readTradeItem(item: SafeItemStack): TradeItem? {
        val lore = item.getLoreComponent().map { it.formattedTextCompatLessResets() }
        if (TRADE_LINE !in lore) return null

        val costs = lore.readLoreCosts(item.hoverName.formattedTextCompatLeadingWhiteLessResets())
        if (costs.isEmpty()) return null

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
            currency != null -> currency.getOwnedAmount()
            else -> internalName.getAmountInInventory().toLong()
        }
        val covered = owned != null && owned >= amount

        val suffix = when {
            owned == null -> ""
            covered -> " §a✔"
            else -> " §8${owned.addSeparators()}§7/§8${amount.addSeparators()}"
        }
        val sacks = if (currency == null) internalName.getAmountInSacksOrNull() ?: 0 else 0
        val sackText = if (sacks > 0) " §7(sacks: §a${sacks.addSeparators()}§7)" else ""

        // currencies other than coins have no price, getPriceName would show a "(0)" behind them
        val name = if (currency != null && currency.coinValue == null) {
            currency.formatAmount(amount)
        } else {
            internalName.getPriceName(amount)
        }

        return CostLine(entry.rawLine, name + suffix + sackText, covered, owned != null)
    }

    @HandleEvent
    private fun onToolTip(event: ToolTipTextEvent) {
        if (!config.costBreakdown) return
        val slot = event.slot?.index ?: return
        val tradeItem = tradeItems[slot] ?: return

        for ((index, component) in event.toolTip.withIndex()) {
            val line = component.formattedTextCompatLessResets()
            if (LoreCostUtils.isCostHeader(line)) {
                // the inline form writes the only cost entry into the header line itself
                val inlineCost = tradeItem.lines.firstOrNull { line.contains(it.rawLine) }
                val newLine = inlineCost?.let { line.replace(it.rawLine, it.text) } ?: line
                event.toolTip[index] = Component.literal(newLine + tradeItem.headerSuffix)
                continue
            }
            val costLine = tradeItem.lines.firstOrNull { it.rawLine == line } ?: continue
            event.toolTip[index] = Component.literal(costLine.text)
        }
    }

    @HandleEvent
    private fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightAffordable || tradeItems.isEmpty()) return

        for (slot in event.container.slots) {
            val tradeItem = tradeItems[slot.index] ?: continue
            if (tradeItem.canAfford) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && (config.highlightAffordable || config.costBreakdown)
}
