package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import net.minecraft.inventory.Slot
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// Please use LorenzToolTipEvent over ItemTooltipEvent if no special EventPriority is necessary
class ToolTipData {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        val toolTip = event.toolTip ?: return
        val slot = lastSlot ?: return
        LorenzToolTipEvent(slot, event.itemStack, toolTip).postAndCatch()
    }

    companion object {
        var lastSlot: Slot? = null
    }
}
