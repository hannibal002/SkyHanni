package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarBestSellMethod {

    companion object {
        private var display = ""
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.CloseWindowEvent) {
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
            var name = bazaarItem.displayName
            name = BazaarApi.getCleanBazaarName(name)
            val data = BazaarApi.getBazaarDataForName(name) ?: return ""

            var having = 0
            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber == slot.slotIndex) continue
                if (slot.stack == null) continue
                val stack = slot.stack
                var displayName = stack.displayName
                if (displayName.endsWith("Enchanted Book")) {
                    displayName = stack.getLore()[0]
                }
                if (BazaarApi.getCleanBazaarName(displayName) == name) {
                    having += stack.stackSize
                }
            }

            if (having <= 0) return ""

            val totalDiff = (data.buyPrice - data.sellPrice) * having
            val result = NumberUtil.format(totalDiff.toInt())

            return "§b$name§f sell difference: §e$result coins"
        } catch (e: Error) {
            e.printStackTrace()
            return ""
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.bazaar.bestSellMethodPos.renderString(display)
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.bazaar.bestSellMethod
    }
}