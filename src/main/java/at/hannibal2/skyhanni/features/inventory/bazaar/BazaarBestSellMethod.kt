package at.hannibal2.hanni.features.inventory.bazaar

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.bazaar.BazaarOpenedProductEvent
import at.hannibal2.hanni.features.inventory.bazaar.BazaarApi.getBazaarDataOrError
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.system.PlatformUtils
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object BazaarBestSellMethod {
    private val config get() = HanniMod.feature.inventory.bazaar

    private var display = ""

    // Working with the last clicked item manually because
    // the open inventory event happen while the recent clicked item in the inventory is not in the inventory or in the cursor slot
    private var lastClickedItem: ItemStack? = null
    private var nextCloseWillResetItem = false

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = ""
        if (lastClickedItem != null) {
            if (nextCloseWillResetItem) {
                lastClickedItem = null
            }
            nextCloseWillResetItem = !nextCloseWillResetItem
        }
    }

    @HandleEvent
    fun onBazaarOpenedProduct(event: BazaarOpenedProductEvent) {
        if (!isEnabled()) return
        display = updateDisplay(event.openedProduct)

        // on 1.21 NeuInternalName.getAmountInInventory() does not include the item currently clicked at
        if (!PlatformUtils.IS_LEGACY) {
            DelayedRun.runDelayed(300.milliseconds) {
                if (display.isEmpty()) {
                    display = updateDisplay(event.openedProduct)
                }
            }
        }
    }

    private fun updateDisplay(internalName: NeuInternalName?): String {
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
        val totalDiff = (data.instantBuyPrice - data.instantSellPrice) * having
        val result = totalDiff.toInt().shortFormat()

        val name = internalName.repoItemName
        return "$name§7 sell difference: §6$result coins"
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        config.bestSellMethodPos.renderString(display, posLabel = "Bazaar Best Sell Method")
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        lastClickedItem = event.slot?.stack
        nextCloseWillResetItem = false
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.bestSellMethod
}
