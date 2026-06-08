package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class BaseFovEvent(val originalFov: Int): SkyHanniEvent() {
    var additive = 0f
    var multiplier = 1f
}
