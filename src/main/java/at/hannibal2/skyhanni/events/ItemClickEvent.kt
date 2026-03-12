package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.WorldClickType
import net.minecraft.world.item.ItemStack

// Left or right click into the world, with the item in hand
class ItemClickEvent(itemInHand: ItemStack?, clickType: WorldClickType) : WorldClickEvent(itemInHand, clickType)
