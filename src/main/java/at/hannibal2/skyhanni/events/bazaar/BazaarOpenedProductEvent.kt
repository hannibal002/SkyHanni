package at.hannibal2.hanni.events.bazaar

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.utils.NeuInternalName

class BazaarOpenedProductEvent(val openedProduct: NeuInternalName?, val inventoryOpenEvent: InventoryFullyOpenedEvent) : HanniEvent()
