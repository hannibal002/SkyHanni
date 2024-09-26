package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzRarity

class PetChangeEvent(val petName: String, val petLevel: Int, val petRarity: LorenzRarity) : SkyHanniEvent()
