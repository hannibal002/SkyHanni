package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class ItemInHandChangeEvent(val internalName: String, val stack: ItemStack?) : LorenzEvent()