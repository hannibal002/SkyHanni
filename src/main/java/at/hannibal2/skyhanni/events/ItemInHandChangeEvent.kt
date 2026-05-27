package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.NeuInternalName

@PrimaryFunction("onItemInHandChange")
class ItemInHandChangeEvent(val newItem: NeuInternalName, val oldItem: NeuInternalName) : SkyHanniEvent()
