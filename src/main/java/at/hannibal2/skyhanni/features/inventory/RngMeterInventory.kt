package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RngMeterInventory {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        val chestName = InventoryUtils.openInventoryName()

        val stack = event.stack
        if (SkyHanniMod.feature.inventory.rngMeterFloorName && chestName == "Catacombs RNG Meter") {
            val name = stack.name ?: return
            if (name.removeColor() == "RNG Meter") {
                event.stackTip = stack.getLore()[0].between("(", ")")
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val chestName = InventoryUtils.openInventoryName()
        if (SkyHanniMod.feature.inventory.rngMeterNoDrop && chestName == "Catacombs RNG Meter") {
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                val stack = slot.stack
                if (stack.getLore().any { it.contains("You don't have an RNG drop") }) {
                    slot highlight LorenzColor.RED
                }
            }
        }

        if (SkyHanniMod.feature.inventory.rngMeterSelectedDrop && chestName.endsWith(" RNG Meter")) {
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                val stack = slot.stack
                if (stack.getLore().any { it.contains("§a§lSELECTED") }) {
                    slot highlight LorenzColor.YELLOW
                }
            }
        }
    }
}