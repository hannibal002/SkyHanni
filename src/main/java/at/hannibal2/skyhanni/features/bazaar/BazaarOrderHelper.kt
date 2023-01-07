package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarOrderHelper {

    companion object {
        fun isBazaarOrderInventory(inventoryName: String): Boolean = when (inventoryName) {
            "Your Bazaar Orders" -> true
            "Co-op Bazaar Orders" -> true
            else -> false
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.bazaar.orderHelper) return
        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val inventoryName = chest.getInventoryName()
        if (!isBazaarOrderInventory(inventoryName)) return

        out@ for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            val stack = slot.stack
            val itemName = stack.name ?: continue

            val isSelling = itemName.startsWith("§6§lSELL ")
            val isBuying = itemName.startsWith("§a§lBUY ")
            if (!isSelling && !isBuying) continue

            val rawName = itemName.split(if (isBuying) "BUY " else "SELL ")[1]
            val bazaarName = BazaarApi.getCleanBazaarName(rawName)
            val data = BazaarApi.getBazaarDataForName(bazaarName) ?: return

            val itemLore = stack.getLore()
            for (line in itemLore) {
                if (line.startsWith("§7Filled:")) {
                    if (line.endsWith(" §a§l100%!")) {
                        slot highlight LorenzColor.GREEN
                        continue@out
                    }
                }
                if (line.startsWith("§7Price per unit:")) {
                    var text = line.split(": §6")[1]
                    text = text.substring(0, text.length - 6)
                    text = text.replace(",", "")
                    val price = text.toDouble()
                    if (isSelling && price > data.buyPrice || isBuying && price < data.sellPrice) {
                        slot highlight LorenzColor.GOLD
                        continue@out
                    }
                }
            }
        }
    }
}
