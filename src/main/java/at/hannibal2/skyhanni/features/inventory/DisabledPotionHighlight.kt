package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DisabledPotionHighlight {

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDraw(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        val chestName = InventoryUtils.openInventoryName()
        if (SkyHanniMod.feature.inventory.disabledPotionHighlight) {
            if (chestName.startsWith("Toggle Potion Effects")) {
                for (slot in InventoryUtils.getItemsInOpenChest()) {
                    val stack = slot.stack
                    val lore = stack.getLore()

                    if (lore.any { it == "§7Currently: §cDISABLED" }) {
                        slot highlight LorenzColor.RED
                    }
                }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.disabledPotionHighlight
}