package at.hannibal2.skyhanni.events

import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class LorenzToolTipEvent(val slot: Slot, val itemStack: ItemStack, var toolTip: MutableList<String>) : LorenzEvent() {
    fun toolTipRemovedPrefix() = toolTip.map { it.removePrefix("§5§o") }
}
