package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BazaarOrderHelper {
    private val bazaarItemNamePattern = "§.§l(?<type>BUY|SELL) (?<name>.*)".toPattern()
    private val filledPattern = "§7Filled: §6.*§7/.* §a§l100%!".toPattern()
    private val pricePattern = "§7Price per unit: §6(?<number>.*) coins".toPattern()

    companion object {
        fun isBazaarOrderInventory(inventoryName: String): Boolean = when (inventoryName) {
            "Your Bazaar Orders" -> true
            "Co-op Bazaar Orders" -> true
            else -> false
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.bazaar.orderHelper) return
        if (event.gui !is GuiChest) return

        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val inventoryName = chest.getInventoryName()
        if (!isBazaarOrderInventory(inventoryName)) return

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            val itemName = slot.stack.name ?: continue
            val matcher = bazaarItemNamePattern.matcher(itemName)
            if (!matcher.matches()) continue

            val buyOrSell = matcher.group("type").let { (it == "BUY") to (it == "SELL") }
            if (buyOrSell.let { !it.first && !it.second }) continue

            highlightItem(matcher.group("name"), slot, buyOrSell)
        }
    }

    private fun highlightItem(itemName: String, slot: Slot, buyOrSell: Pair<Boolean, Boolean>) {
        val data = BazaarApi.getBazaarDataByName(itemName)
        if (data == null) {
            LorenzUtils.debug("Bazaar data is null for bazaarItemName '$itemName'")
            return
        }

        val itemLore = slot.stack.getLore()
        for (line in itemLore) {
            if (filledPattern.matcher(line).matches()) {
                slot highlight LorenzColor.GREEN
            }

            val matcher = pricePattern.matcher(line)
            if (matcher.matches()) {
                val price = matcher.group("number").replace(",", "").toDouble()
                if (buyOrSell.first && price < data.sellPrice || buyOrSell.second && price > data.buyPrice) {
                    slot highlight LorenzColor.GOLD
                    break
                }
            }
        }
    }
}
