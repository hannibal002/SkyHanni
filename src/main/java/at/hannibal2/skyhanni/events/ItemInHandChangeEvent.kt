package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.NeuInternalName

class ItemInHandChangeEvent(val newItem: NeuInternalName, val oldItem: NeuInternalName) : HanniEvent()
