package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack

data class OwnInventoryItemUpdateEvent(val itemStack: SafeItemStack, val slot: Int) : SkyHanniEvent()
