package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import net.minecraft.inventory.Slot

// Please use LorenzToolTipEvent over ItemTooltipEvent if no special EventPriority is necessary
object ToolTipData {
    fun onTooltip(toolTip: MutableList<String>): List<String> {
        val slot = lastSlot ?: return toolTip
        val itemStack = slot.stack ?: return toolTip
        try {
            if (LorenzToolTipEvent(slot, itemStack, toolTip).postAndCatch()) {
                toolTip.clear()
            }
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e, "Error in item tool tip parsing or rendering detected",
                "toolTip" to toolTip,
                "slot" to slot,
                "slotNumber" to slot.slotNumber,
                "slotIndex" to slot.slotIndex,
                "itemStack" to itemStack,
                "name" to itemStack.name,
                "internal name" to itemStack.getInternalName(),
                "lore" to itemStack.getLore(),
            )
        }
        return toolTip
    }

    var lastSlot: Slot? = null

}
