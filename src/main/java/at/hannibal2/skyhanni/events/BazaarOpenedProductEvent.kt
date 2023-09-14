package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.NEUInternalName

class BazaarOpenedProductEvent(val openedProduct: NEUInternalName, val inventoryOpenEvent: InventoryFullyOpenedEvent) :
    LorenzEvent()