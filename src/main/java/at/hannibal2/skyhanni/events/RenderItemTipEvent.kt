package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class RenderItemTipEvent(
    val stack: ItemStack,
    var stackTip: String = "",
    var offsetX: Int = 0,
    var offsetY: Int = 0,
    var alignLeft: Boolean = true,
) : LorenzEvent()