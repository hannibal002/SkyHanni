package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * Event triggered when the accessory bag is opened or updated.
 */
@PrimaryFunction("onAccessoryBagUpdate")
class AccessoryBagUpdateEvent(
    val inventoryName: String,
    val inventoryItems: Map<Int, SafeItemStack>,
) : SkyHanniEvent()
