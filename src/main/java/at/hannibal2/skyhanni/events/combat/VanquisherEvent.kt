package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.VanquisherApi.VanquisherData

sealed class VanquisherEvent(val vanquisher: VanquisherData) : SkyHanniEvent() {
    /** Gets called when a Vanquisher's mob disappears, no matter the cause. */
    class DeSpawn(vanquisher: VanquisherData) : VanquisherEvent(vanquisher)

    /** Gets called when a Vanquisher dies. */
    class Death(vanquisher: VanquisherData) : VanquisherEvent(vanquisher)

    /** Gets called when a Vanquisher is initially detected. */
    class Spawn(vanquisher: VanquisherData) : VanquisherEvent(vanquisher)
}
