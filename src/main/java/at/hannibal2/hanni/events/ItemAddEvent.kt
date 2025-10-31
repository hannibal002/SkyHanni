package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import at.hannibal2.hanni.data.ItemAddManager
import at.hannibal2.hanni.utils.NeuInternalName

class ItemAddEvent(val internalName: NeuInternalName, val amount: Int, val source: ItemAddManager.Source) : CancellableHanniEvent()
