package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import com.google.gson.JsonPrimitive

@SkyHanniModule
object SkyMartCopperPrice {

    private var display = emptyList<Renderable>()
    private val config get() = GardenApi.config.skyMart

    var inInventory = false

    private val COPPER_ITEM = SkyblockCurrency.COPPER.internalName

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!event.inventoryName.matches(Regex("(\\(\\d+/\\d+\\) )?SkyMart .*"))) return

        DelayedRun.runOrNextTick {
            inInventory = true
            val table = mutableListOf<DisplayTableEntry>()
            for ((slot, item) in event.inventoryItems) {
                val costs = item.readLoreCosts()
                val copper = costs.firstOrNull { it.internalName == COPPER_ITEM }?.amount
                    ?: continue
                val otherItemsPrice = costs
                    .filter { it.internalName != COPPER_ITEM }
                    .sumOf { it.internalName.getPrice() * it.amount }
                    .takeIf { it != 0.0 }

                val internalName = item.getInternalName()
                val itemPrice = internalName.getPriceOrNull(config.priceSource) ?: continue
                val profit = itemPrice - (otherItemsPrice ?: 0.0)

                val factor = profit / copper
                val perFormat = factor.shortFormat()

                val itemName = item.repoItemName
                val hover = buildList {
                    add(itemName)
                    add("")
                    add("§7Item price: §6${itemPrice.shortFormat()} ")
                    otherItemsPrice?.let {
                        add("§7Additional cost: §6${it.shortFormat()} ")
                    }
                    add("§7Profit per purchase: §6${profit.shortFormat()} ")
                    add("")
                    add("§7Copper amount: §c${copper.addSeparators()} ")
                    add("§7Profit per copper: §6$perFormat ")
                }
                table.add(
                    DisplayTableEntry(
                        "$itemName§f:".asComponent(),
                        "§6§l$perFormat".asComponent(),
                        factor,
                        internalName,
                        hover.mapToComponents(),
                        highlightsOnHoverSlots = listOf(slot),
                    ),
                )
            }
            val newList = mutableListOf<Renderable>()
            newList.addString("§eCoins per Copper§f:")
            newList.add(RenderableUtils.fillTable(table, padding = 5, itemScale = config.itemScale))
            display = newList
        }
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
    }

    @HandleEvent
    private fun onChestGuiRender() {
        if (inInventory) {
            config.copperPricePos.renderRenderables(
                display,
                extraSpace = 5,
                posLabel = "SkyMart Copper Price",
            )
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.skyMartCopperPrice", "garden.skyMart.copperPrice")
        event.move(3, "garden.skyMartCopperPriceAdvancedStats", "garden.skyMart.copperPriceAdvancedStats")
        event.move(3, "garden.skyMartCopperPricePos", "garden.skyMart.copperPricePos")
        event.transform(32, "garden.skyMart.itemScale") {
            JsonPrimitive((it.asDouble / 1.851).roundTo(1))
        }
    }

    private fun isEnabled() = GardenApi.inGarden() && config.copperPrice
}
