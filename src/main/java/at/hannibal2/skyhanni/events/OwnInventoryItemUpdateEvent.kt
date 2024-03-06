package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

data class OwnInventoryItemUpdateEvent(val itemStack: ItemStack) : LorenzEvent()
