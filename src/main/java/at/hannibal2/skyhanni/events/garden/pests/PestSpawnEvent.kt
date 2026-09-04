package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.api.event.SkyHanniEvent


/**
 * When a pest spawn message gets detected while in the garden.
 */
class PestSpawnEvent(val amountPests: Int?, val plotNames: List<String>) : SkyHanniEvent() {
    init {
        require(amountPests == null || amountPests > 0) { "amountPests must be a positive integer or null" }
    }
}
