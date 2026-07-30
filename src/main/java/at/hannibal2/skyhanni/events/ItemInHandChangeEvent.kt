package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * Gets fired when the item held in the player's main hand changes.
 * This includes both switching to a different inventory slot and the item stack in the current slot being replaced.
 *
 * @param oldSlot The inventory slot index that was selected before the change.
 * @param oldStack The item stack that was held before the change.
 * @param newSlot The inventory slot index that is selected after the change.
 * @param newStack The item stack that is held after the change.
 */
@Thread(RENDER)
@PrimaryFunction("onItemInHandChange")
data class ItemInHandChangeEvent(
    val oldSlot: Int,
    val oldStack: SafeItemStack,
    val newSlot: Int,
    val newStack: SafeItemStack,
) : SkyHanniEvent() {

    /** The internal name of the item that was held before the change. */
    val oldItem = oldStack.getInternalName()

    /** The internal name of the item that is held after the change. */
    val newItem = newStack.getInternalName()
}
