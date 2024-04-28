package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class RenderGuiItemOverlayEvent(
    val stack: ItemStack?,
    val x: Int,
    val y: Int,
) : LorenzEvent()
