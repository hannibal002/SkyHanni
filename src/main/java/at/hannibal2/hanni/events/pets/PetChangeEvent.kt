package at.hannibal2.hanni.events.pets

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.PetData

class PetChangeEvent(val pet: PetData) : HanniEvent()
