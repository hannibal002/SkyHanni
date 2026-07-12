package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import at.hannibal2.skyhanni.utils.SafeItemStack

@Thread(RENDER)
data class OwnInventoryArmorUpdateEvent(val itemStack: SafeItemStack, val slot: Int) : SkyHanniEvent()

@Thread(RENDER)
data class OwnInventoryItemUpdateEvent(val itemStack: SafeItemStack, val slot: Int) : SkyHanniEvent()

@Thread(RENDER)
data class OwnInventoryMenuUpdateEvent(val itemStack: SafeItemStack) : SkyHanniEvent()
