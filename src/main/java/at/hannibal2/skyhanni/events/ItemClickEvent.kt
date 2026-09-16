package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * Fired when the player left or right clicks in the world, before the click is dispatched to the more
 * specific `BlockClickEvent` or `EntityClickEvent`. Subscribe to those instead when the target matters.
 *
 * Also fired while the player keeps breaking and the targeted block position changes, so it does not always
 * correspond to a fresh press of the mouse button.
 *
 * Cancelling stops the click from being handled any further, and the follow-up event for a block or an
 * entity is posted as already canceled.
 *
 * @param itemInHand The item held by the player at the time of the click, or null if the hand is empty.
 * @param clickType Whether it was a left or a right click.
 */
@PrimaryFunction("onItemClick")
class ItemClickEvent(itemInHand: SafeItemStack?, clickType: InteractClickType) : WorldClickEvent(itemInHand, clickType)
