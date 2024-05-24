package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.BazaarOpenedProductEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.Companion.getBazaarDataOrError
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarBestSellMethod {
    private val config get() = SkyHanniMod.feature.inventory.bazaar

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
        var having = internalName.getAmountInInventory()
        lastClickedItem?.let {
            if (it.getInternalName() == internalName) {
                having += it.stackSize
            }
        }
        if (having <= 0) return ""

        val data = internalName.getBazaarDataOrError()
        val totalDiff = (data.buyPrice - data.sellPrice) * having
        val result = NumberUtil.format(totalDiff.toInt())

        val name = internalName.itemName
        return "$name§7 sell difference: §6$result coins"
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        config.bestSellMethodPos.renderString(display, posLabel = "Bazaar Best Sell Method")
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        lastClickedItem = event.slot?.stack
        nextCloseWillResetItem = false
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.bestSellMethod
}
