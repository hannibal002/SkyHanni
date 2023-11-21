package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.utils.NEUInternalName

class ItemAddInInventoryEvent(val internalName: NEUInternalName, val amount: Int) : LorenzEvent()
