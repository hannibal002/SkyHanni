package at.hannibal2.hanni.events

import at.hannibal2.hanni.data.ClickType
import net.minecraft.item.ItemStack

// Left or right click into the world, with the item in hand
class ItemClickEvent(itemInHand: ItemStack?, clickType: ClickType) : WorldClickEvent(itemInHand, clickType)
