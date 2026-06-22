package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.SafeItemStack

@PrimaryFunction("onItemInHandChange")
data class ItemInHandChangeEvent(
    val oldSlot: Int,
    val oldStack: SafeItemStack,
    val newSlot: Int,
    val newStack: SafeItemStack,
) : SkyHanniEvent() {
    val oldItem = oldStack.getInternalName()
    val newItem = newStack.getInternalName()
}
