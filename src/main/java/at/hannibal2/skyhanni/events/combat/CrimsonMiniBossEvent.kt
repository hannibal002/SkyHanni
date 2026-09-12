package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.nether.miniboss.CrimsonMiniBoss

/** Gets called when a Crimson Isle miniboss is spawning or killed. */
sealed class CrimsonMiniBossEvent(val miniBoss: CrimsonMiniBoss) : SkyHanniEvent() {
    /** Gets called when a Crimson Isle miniboss is spawning soon. */
    class Spawning(miniBoss: CrimsonMiniBoss) : CrimsonMiniBossEvent(miniBoss)

    /** Gets called when a Crimson Isle miniboss is killed. */
    class Death(miniBoss: CrimsonMiniBoss) : CrimsonMiniBossEvent(miniBoss)
}
