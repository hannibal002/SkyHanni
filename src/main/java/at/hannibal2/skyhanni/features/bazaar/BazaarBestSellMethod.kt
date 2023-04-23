package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarBestSellMethod {
    private var display = ""

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display = ""
    }

    @SubscribeEvent
    fun onGuiDraw(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (!isEnabled()) return
        display = getNewText(event)
    }

    private fun getNewText(event: GuiScreenEvent.DrawScreenEvent.Post): String {
        try {
            if (event.gui !is GuiChest) return ""
            val chest = (event.gui as GuiChest).inventorySlots as ContainerChest

            val inv = chest.lowerChestInventory ?: return ""

            val buyInstantly = inv.getStackInSlot(10)
            if (buyInstantly == null || buyInstantly.displayName != "§aBuy Instantly") return ""
            val bazaarItem = inv.getStackInSlot(13) ?: return ""

            val internalName = NEUItems.getInternalNameOrNull(bazaarItem.displayName) ?: return ""

            var having = 0
            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber == slot.slotIndex) continue
                val stack = slot.stack ?: continue
                if (internalName == stack.getInternalName()) {
                    having += stack.stackSize
                }
            }

            if (having <= 0) return ""

            val data = BazaarApi.getBazaarDataByInternalName(internalName) ?: return ""
            val totalDiff = (data.buyPrice - data.sellPrice) * having
            val result = NumberUtil.format(totalDiff.toInt())

            val name = NEUItems.getItemStack(internalName).nameWithEnchantment
            return "§b$name§f sell difference: §e$result coins"
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

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.bazaar.bestSellMethod
    }
}