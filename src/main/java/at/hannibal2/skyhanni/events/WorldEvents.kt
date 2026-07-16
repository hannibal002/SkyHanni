package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzVec

/**
 * Base class for world-related events.
 *
 * Used by TrackerCommands and other built-in utilities that need to debug or interact
 * with world events based on their location, such as particles and sounds.
 */
abstract class WorldEvent : SkyHanniEvent() {
    /**
     * The location of this event.
     */
    abstract val location: LorenzVec
}

/**
 * Base class for cancellable world events.
 */
abstract class CancellableWorldEvent : WorldEvent(), SkyHanniEvent.Cancellable
