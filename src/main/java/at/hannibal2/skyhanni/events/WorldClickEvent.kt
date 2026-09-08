package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * The base class for every click into the world. Never fired on its own, only as one of its subclasses:
 * `BlockClickEvent` for a click on a block, `EntityClickEvent` for a click on an entity, and `ItemClickEvent`
 * for a click into the world with the item in hand.
 *
 * Subscribe to this class to catch every kind of click, or to a single subclass for one specific kind.
 *
 * @param itemInHand The item held by the player at the time of the click, or null if the hand is empty.
 * @param clickType Whether it was a left or a right click.
 */
open class WorldClickEvent(val itemInHand: SafeItemStack?, val clickType: InteractClickType) : CancellableSkyHanniEvent()
