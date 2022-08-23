package at.hannibal2.skyhanni.features.anvil

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
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
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.inventory.anvilCombineHelper) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        if (chestName != "Anvil") return

        val matchLore = mutableListOf<String>()
//        var compareItem: ItemStack? = null

        for (slot in chest.inventorySlots) {
            if (slot == null) continue

            if (slot.slotNumber != slot.slotIndex) continue
            if (slot.stack == null) continue

            if (slot.slotNumber == 29) {
//                slot highlight LorenzColor.GREEN
                val lore = slot.stack.getLore()
//                compareItem = slot.stack
                matchLore.addAll(lore)
                break
//            } else if (slot.slotIndex == 29) {
//                slot highlight LorenzColor.YELLOW
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

//            if (compareItem == slot.stack) {
//                slot highlight LorenzColor.GREEN
//            } else if (compareItem.metadata == slot.stack.metadata) {
//                slot highlight LorenzColor.YELLOW
//            }

//            if (slot.slotNumber == 3) {
////                slot highlight LorenzColor.GREEN
////            } else if (slot.slotIndex == 4) {
////                slot highlight LorenzColor.YELLOW
////            }
//            }
        }
    }
}