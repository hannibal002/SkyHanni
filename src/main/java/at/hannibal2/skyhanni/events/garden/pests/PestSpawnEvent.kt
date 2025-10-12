package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class PestSpawnEvent(val amountPests: Int?, val plotNames: List<String>) : SkyHanniEvent() {
    init {
        if (amountPests != null && amountPests < 1) {
            throw IllegalArgumentException("amountPests must be a positive integer or null")
        }
    }
}
