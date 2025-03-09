package at.hannibal2.skyhanni.events.yearofthepig

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzVec

class ShinyOrbChargedEvent(
    val location: LorenzVec? = null,
    val orbEntityId: Int? = null
) : SkyHanniEvent()
