package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object HighlightOnHoverSlot {
    val currentSlots = mutableMapOf<Pair<Int, Int>, List<Int>>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        currentSlots.clear()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        currentSlots.clear()
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDrawBackground(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val list = currentSlots.flatMapTo(mutableSetOf()) { it.value }
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotNumber in list) {
                slot highlight LorenzColor.GREEN
            }
        }
    }
}
