package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BazaarOpenedProductEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getNameWithEnchantment
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarBestSellMethod {
    private var display = ""

    // Working with the last clicked item manually because
    // the open inventory event happen while the recent clicked item in the inventory is not in the inventory or in the cursor slot
    private var lastClickedItem: ItemStack? = null
    private var nextCloseWillResetItem = false

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = ""
        if (lastClickedItem != null) {
            if (nextCloseWillResetItem) {
                lastClickedItem = null
            }
            nextCloseWillResetItem = !nextCloseWillResetItem
        }
    }

    @SubscribeEvent
    fun onBazaarOpenedProduct(event: BazaarOpenedProductEvent) {
        if (!isEnabled()) return
        display = updateDisplay(event.openedProduct)
    }

    @SubscribeEvent
    fun onSellButtonHover(event: ItemTooltipEvent) {
        if(!isEnabled()) return
        if (event.itemStack?.name?.let { it.removeColor() } != "Sell Inventory Now") return

        var lores = event.toolTip
        var startIndex = lores.indexOfFirst { lore -> lore.removeColor() == "" } + 1
        var endIndex = lores.indexOfLast { lore -> lore.removeColor() == "" } - 3
        if (endIndex < startIndex) return

        var sum = 0.0
        for (i in startIndex..endIndex) {
            var lore = lores[i]
            var words = lore.removeColor().split(" ")
            var amount = words[1].substringBeforeLast('x').toInt()
            var itemName = words.subList(2, words.size - 3).joinToString(" ")

            var internalName = NEUItems.getInternalNameFromItemName(itemName)
            var data = internalName.getBazaarData()
            sum += getSellDifference(internalName, amount)
        }
        lores.add(lores.size - 1, "§7Total sell difference: §6${NumberUtil.format(sum.toInt())} coins")
    }

    private fun getSellDifference(internalName: NEUInternalName, amount: Int): Double {
        val data = internalName.getBazaarData() ?: return 0.0
        val totalDiff = (data.buyPrice - data.sellPrice) * amount
        return totalDiff
    }

    private fun updateDisplay(internalName: NEUInternalName): String {
        try {
            var having = InventoryUtils.countItemsInLowerInventory { it.getInternalName() == internalName }
            lastClickedItem?.let {
                if (it.getInternalName() == internalName) {
                    having += it.stackSize
                }
            }
            if (having <= 0) return ""

            val sellDifference = getSellDifference(internalName, having)
            val result = NumberUtil.format(sellDifference.toInt())

            val name = internalName.getNameWithEnchantment()
            return "$name§7 sell difference: §6$result coins"
        } catch (e: Error) {
            e.printStackTrace()
            return ""
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.bazaar.bestSellMethodPos.renderString(display, posLabel = "Bazaar Best Sell Method")
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        lastClickedItem = event.slot?.stack
        nextCloseWillResetItem = false
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.bazaar.bestSellMethod
}
