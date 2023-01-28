package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import net.minecraft.item.ItemStack

class ItemClickInHandEvent(val clickType: ClickType, val itemInHand: ItemStack?): LorenzEvent()