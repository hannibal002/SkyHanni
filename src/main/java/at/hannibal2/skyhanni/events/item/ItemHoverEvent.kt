package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

class ItemHoverEvent(context: DrawContext, val itemStack: ItemStack, private val toolTip0: MutableList<String>) :
    RenderingSkyHanniEvent(context) {
    var toolTip
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0
}
