package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.item.ItemStack

data class ItemCollectEvent(val itemStack: ItemStack, val slot: Int) : SkyHanniEvent()
