package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

/**
 * Note: This event is async and may not be executed on the main minecraft thread.
 */
data class OwnInventoryItemUpdateEvent(val itemStack: ItemStack) : LorenzEvent()