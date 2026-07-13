package at.hannibal2.skyhanni.events.pets

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.data.PetData

@Thread(NETWORK, RENDER)
class PetChangeEvent(val pet: PetData) : SkyHanniEvent()
