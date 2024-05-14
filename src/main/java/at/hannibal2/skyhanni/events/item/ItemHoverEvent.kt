package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.events.LorenzEvent
import net.minecraft.item.ItemStack

class ItemHoverEvent(val itemStack: ItemStack, val toolTip: MutableList<String>) : LorenzEvent()
