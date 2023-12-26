package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BazaarOpenedProductEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getNameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
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

    private fun updateDisplay(internalName: NEUInternalName?): String {
        if (internalName == null) {
            return "§cUnknown Bazaar item!"
        }
        try {
            var having = InventoryUtils.countItemsInLowerInventory { it.getInternalName() == internalName }
            lastClickedItem?.let {
                if (it.getInternalName() == internalName) {
                    having += it.stackSize
                }
            }
            if (having <= 0) return ""

            val data = internalName.getBazaarData() ?: return ""
            val totalDiff = (data.buyPrice - data.sellPrice) * having
            val result = NumberUtil.format(totalDiff.toInt())

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
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        lastClickedItem = event.slot?.stack
        nextCloseWillResetItem = false
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.bazaar.bestSellMethod
}
