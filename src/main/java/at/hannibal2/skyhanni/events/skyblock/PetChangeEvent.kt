package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.PetData

class PetChangeEvent(val oldPet: PetData, val newPet: PetData) : SkyHanniEvent()
