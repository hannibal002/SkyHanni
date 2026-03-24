package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.utils.SafeItemStack

// Left or right click into the world, with the item in hand
class ItemClickEvent(itemInHand: SafeItemStack?, clickType: ClickType) : WorldClickEvent(itemInHand, clickType)
