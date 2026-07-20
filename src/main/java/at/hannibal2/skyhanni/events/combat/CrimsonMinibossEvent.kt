package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.nether.miniboss.CrimsonMiniBoss

/** Gets called when a Crimson Isle miniboss is spawning or killed. */
sealed class CrimsonMinibossEvent(val miniboss: CrimsonMiniBoss) : SkyHanniEvent() {
    /** Gets called when a Crimson Isle miniboss is spawning soon. */
    class Spawning(miniboss: CrimsonMiniBoss) : CrimsonMinibossEvent(miniboss)

    /** Gets called when a Crimson Isle miniboss is killed. */
    class Death(miniboss: CrimsonMiniBoss) : CrimsonMinibossEvent(miniboss)
}
