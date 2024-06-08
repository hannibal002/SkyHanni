package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.events.LorenzEvent
import net.minecraft.item.ItemStack

class ItemHoverEvent(val itemStack: ItemStack, private val toolTip0: MutableList<String>) : LorenzEvent() {
    var toolTip
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0
}
