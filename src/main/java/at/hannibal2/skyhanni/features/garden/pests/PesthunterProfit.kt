package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LoreCostUtils
import at.hannibal2.skyhanni.utils.LoreCostUtils.hasTradeLine
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils

@SkyHanniModule
object PesthunterProfit {

    private val config get() = GardenApi.config.pests.pesthunterShop
    private var display = emptyList<Renderable>()
    private var inInventory = false
    private val PESTS_ITEM = SkyblockCurrency.PESTS.internalName

    fun isInInventory() = inInventory

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.profitEnabled) return
        if (event.inventoryName != "Pesthunter's Wares") return

        DelayedRun.runOrNextTick {
            inInventory = true
            display = buildRenderables(event.inventoryItems)
        }
    }

    private fun buildRenderables(items: Map<Int, SafeItemStack>) = buildList {
        val table = items.mapNotNull { (slot, stack) -> readItem(slot, stack) }
        addString("§ePesthunter Shop Profit")
        add(RenderableUtils.fillTable(table, padding = 5, itemScale = 0.7))
    }

    private fun readItem(slot: Int, item: SafeItemStack): DisplayTableEntry? {
        val lore = item.getLoreComponent().map { it.formattedTextCompatLessResets() }
        if (!lore.hasTradeLine()) return null

        val nameString = item.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val costs = lore.readLoreCosts(nameString)
        val totalCost = getFullCost(costs).takeIf { it >= 0 } ?: return null
        val (name, amount) = ItemUtils.readItemAmount(nameString) ?: return null
        val fixedDisplayName = name.replace("[Lvl 100]", "[Lvl {LVL}]")
        val internalName = NeuInternalName.fromItemNameOrNull(fixedDisplayName)
            ?: item.getInternalName()

        val itemPrice = (internalName.getPrice() * amount).takeIf { it >= 0 } ?: return null

        val profit = itemPrice - totalCost
        val pestsCost = costs.firstOrNull { it.internalName == PESTS_ITEM }?.amount ?: 0L
        val profitPerPest = if (pestsCost > 0) profit / pestsCost else 0.0
        val color = if (profitPerPest > 0) "§6" else "§c"

        val hover = listOf(
            nameString.replace("[Lvl 100]", "[Lvl 1]"),
            "",
            "§7Item price: §6${itemPrice.shortFormat()} ",
            "§7Material cost: §6${totalCost.shortFormat()} ",
            "§7Final profit: §6${profit.shortFormat()} ",
            "§7Profit per pest: §6${profitPerPest.shortFormat()} ",
        )

        return DisplayTableEntry(
            nameString.replace("[Lvl 100]", "[Lvl 1]").asComponent(), // show level 1 hedgehog instead of level 100
            "$color${profitPerPest.shortFormat()}".asComponent(),
            profitPerPest,
            internalName,
            hover.mapToComponents(),
            highlightsOnHoverSlots = listOf(slot),
        )
    }

    private fun getFullCost(costs: List<LoreCostUtils.LoreCostEntry>): Double = costs
        .filter { it.internalName != PESTS_ITEM }
        .sumOf { it.internalName.getPrice() * it.amount }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChestGuiRender() {
        if (!inInventory) return
        config.profitPosition.renderRenderables(
            display,
            extraSpace = 5,
            posLabel = "Pesthunter Profit",
        )
    }
}
