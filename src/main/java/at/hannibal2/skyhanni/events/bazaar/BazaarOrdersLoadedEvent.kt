package at.hannibal2.skyhanni.events.bazaar

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarOrder
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired after one of the bazaar order inventories ("Your Bazaar Orders" or "Co-op Bazaar Orders")
 * was opened and all of its entries were parsed.
 *
 * This event describes an inventory, not the tracked order state. It is fired only when such an
 * inventory is opened. It is not fired when a chat message adds, reduces or removes a tracked
 * order, and it is not fired when the inventory is closed.
 *
 * Use this when the contents of the inventory itself are needed, for example to decorate its slots.
 * To ask how much of a single item is currently on order, use `BazaarApi.getOpenBuyOrderAmount`
 * instead of caching the list from this event.
 *
 * @param orders every order found in the inventory, including the ones belonging to co-op members.
 * Use `BazaarOrder.isOwn` to tell them apart.
 */
@PrimaryFunction("onBazaarOrdersLoaded")
class BazaarOrdersLoadedEvent(val orders: List<BazaarOrder>) : SkyHanniEvent()
