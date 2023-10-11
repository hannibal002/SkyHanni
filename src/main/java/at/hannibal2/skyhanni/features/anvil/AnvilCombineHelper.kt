package at.hannibal2.skyhanni.features.anvil

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AnvilCombineHelper {

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.inventory.anvilCombineHelper) return

        if (event.gui !is GuiChest) return
        val chest = event.gui.inventorySlots as ContainerChest
        val chestName = chest.getInventoryName()

        if (chestName != "Anvil") return

        val matchLore = mutableListOf<String>()

        for (slot in chest.inventorySlots) {
            if (slot == null) continue

            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            if (slot.slotNumber == 29) {
                val lore = slot.stack.getLore()
                matchLore.addAll(lore)
                break
            }
        }

        if (matchLore.isEmpty()) return

        for (slot in chest.inventorySlots) {
            if (slot == null) continue

            if (slot.slotNumber == slot.slotIndex) continue
            if (slot.stack == null) continue


            if (matchLore == slot.stack.getLore()) {
                slot highlight LorenzColor.GREEN
            }
        }
    }
}