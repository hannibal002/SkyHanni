package at.hannibal2.hanni.events.garden.pests

import at.hannibal2.hanni.api.event.HanniEvent


/**
 * When a pest spawn message gets detected while in the garden.
 */
class PestSpawnEvent(val amountPests: Int?, val plotNames: List<String>) : HanniEvent() {
    init {
        require(amountPests == null || amountPests > 0) { "amountPests must be a positive integer or null" }
    }
}
