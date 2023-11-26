package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.utils.NEUInternalName

class ItemAddEvent(val internalName: NEUInternalName, val amount: Int, val source: ItemAddManager.Source) :
    LorenzEvent()
