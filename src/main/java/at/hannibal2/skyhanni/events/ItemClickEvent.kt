package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import net.minecraft.item.ItemStack

class ItemClickEvent(val itemInHand: ItemStack?, val clickType: ClickType) : LorenzEvent()
