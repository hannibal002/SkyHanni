package at.hannibal2.skyhanni.events.garden.pests

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction


/**
 * When a pest spawn message gets detected while in the garden.
 */
@PrimaryFunction("onPestSpawn")
class PestSpawnEvent(val amountPests: Int?, val plotNames: List<String>) : SkyHanniEvent() {
    init {
        require(amountPests == null || amountPests > 0) { "amountPests must be a positive integer or null" }
    }
}
