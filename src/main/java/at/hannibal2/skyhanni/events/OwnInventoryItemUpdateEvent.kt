package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.item.ItemStack

data class OwnInventoryItemUpdateEvent(val itemStack: ItemStack, val slot: Int) : HanniEvent()
