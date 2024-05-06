package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

/**
 * A TooltipEvent that does not have a slot dependency, meaning it can be used for *any* tooltip event
 */
class RawToolTipEvent(var itemStack: ItemStack, private var toolTip0: MutableList<String>) : LorenzEvent() {

    var toolTip
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0
}