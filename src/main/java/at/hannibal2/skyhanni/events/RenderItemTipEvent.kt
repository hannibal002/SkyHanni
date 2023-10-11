package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class RenderItemTipEvent(
    val stack: ItemStack,
    var renderObjects: MutableList<RenderObject>,
) : LorenzEvent() {
    var stackTip = ""
        set(value) {
            renderObjects.add(RenderObject(value, 0, 0))
        }
}

class RenderObject(val text: String, var offsetX: Int = 0, var offsetY: Int = 0)