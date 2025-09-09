package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.screen.slot.Slot
import net.minecraft.item.ItemStack

class ToolTipEvent(val slot: Slot, val itemStack: ItemStack, private val toolTip0: MutableList<String>) : CancellableSkyHanniEvent() {

    var toolTip: MutableList<String>
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0

    fun toolTipRemovedPrefix() = toolTip.map { it.removePrefix("§5§o") }
}
