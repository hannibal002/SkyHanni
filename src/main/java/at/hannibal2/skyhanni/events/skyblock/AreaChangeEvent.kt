package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class AreaChangeEvent(val area: String, val previousArea: String?) : SkyHanniEvent()
