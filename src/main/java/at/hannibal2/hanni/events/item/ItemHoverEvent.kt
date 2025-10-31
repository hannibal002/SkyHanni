package at.hannibal2.hanni.events.item

import at.hannibal2.hanni.api.event.RenderingHanniEvent
import at.hannibal2.hanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

class ItemHoverEvent(context: DrawContext, val itemStack: ItemStack, private val toolTip0: MutableList<String>) :
    RenderingHanniEvent(context) {
    var toolTip
        set(value) {
            toolTip0.clear()
            toolTip0.addAll(value)
        }
        get() = toolTip0
}
