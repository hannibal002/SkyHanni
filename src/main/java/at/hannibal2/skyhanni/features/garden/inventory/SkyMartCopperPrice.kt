package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils.fillTable
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyMartCopperPrice {

    private val copperPattern by RepoPattern.pattern(
        "garden.inventory.skymart.copper",
        "§c(?<amount>.*) Copper"
    )

    private var display = emptyList<List<Any>>()
    private val config get() = GardenAPI.config.skyMart

    companion object {

        var inInventory = false
    }

    private fun ItemStack.loreCosts(): MutableList<NEUInternalName> {
        var found = false
        val list = mutableListOf<NEUInternalName>()
        for (lines in getLore()) {
            if (lines == "§7Cost") {
                found = true
                continue
            }

            if (!found) continue
            if (lines.isEmpty()) return list

            NEUInternalName.fromItemNameOrNull(lines)?.let {
                list.add(it)
            }
        }
        return list
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!event.inventoryName.startsWith("SkyMart ")) return

        inInventory = true
        val table = mutableListOf<DisplayTableEntry>()
        for ((slot, item) in event.inventoryItems) {
            val lore = item.getLore()
            val otherItemsPrice = item.loreCosts().sumOf { it.getPrice() }.takeIf { it != -1.0 }

            for (line in lore) {
                val copper = copperPattern.matchMatcher(line) {
                    group("amount").formatInt()
                } ?: continue

                val internalName = item.getInternalName()
                val lowestBin = internalName.getPriceOrNull() ?: continue
                val profit = lowestBin - (otherItemsPrice ?: 0.0)

                val factor = profit / copper
                val perFormat = NumberUtil.format(factor)

                val itemName = item.itemName
                val hover = buildList {
                    add(itemName)
                    add("")
                    add("§7Item price: §6${NumberUtil.format(lowestBin)} ")
                    otherItemsPrice?.let {
                        add("§7Additional cost: §6${NumberUtil.format(it)} ")
                    }
                    add("§7Profit per purchase: §6${NumberUtil.format(profit)} ")
                    add("")
                    add("§7Copper amount: §c${copper.addSeparators()} ")
                    add("§7Profit per copper: §6${perFormat} ")
                }
                table.add(DisplayTableEntry("$itemName§f:", "§6§l$perFormat", factor, internalName, hover, highlightsOnHoverSlots = listOf(slot)))
            }
        }

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§eCoins per Copper§f:")
        newList.fillTable(table)
        display = newList
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (inInventory) {
            config.copperPricePos.renderStringsAndItems(
                display,
                extraSpace = 5,
                itemScale = config.itemScale,
                posLabel = "SkyMart Copper Price"
            )
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.copperPrice

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.skyMartCopperPrice", "garden.skyMart.copperPrice")
        event.move(3, "garden.skyMartCopperPriceAdvancedStats", "garden.skyMart.copperPriceAdvancedStats")
        event.move(3, "garden.skyMartCopperPricePos", "garden.skyMart.copperPricePos")
    }
}
