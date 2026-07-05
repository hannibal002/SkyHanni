package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack

class AccessoryBagUpdateEvent(
    val inventoryName: String,
    val inventoryItems: Map<Int, SafeItemStack>,
) : SkyHanniEvent()
