package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzVec

/**
 * This class is a placeholder for world-related events.
 */
abstract class CancellableWorldEvent : WorldEvent(), SkyHanniEvent.Cancellable

abstract class WorldEvent : SkyHanniEvent() {
    abstract val location: LorenzVec
}
