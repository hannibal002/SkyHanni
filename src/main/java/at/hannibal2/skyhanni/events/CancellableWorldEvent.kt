package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import at.hannibal2.hanni.utils.LorenzVec

/**
 * This class is a placeholder for world-related events.
 */
abstract class CancellableWorldEvent : CancellableHanniEvent() {
    abstract val location: LorenzVec
}
