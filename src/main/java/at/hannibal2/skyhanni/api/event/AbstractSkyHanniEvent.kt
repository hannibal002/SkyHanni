package at.hannibal2.skyhanni.api.event

/**
 * Abstract base class for all SkyHanni events.
 *
 * Individual event types should not inherit from this directly;
 * they should inherit from [SkyHanniEvent] or [AsyncSkyHanniEvent] instead.
 */
abstract class AbstractSkyHanniEvent protected constructor() {
    // TODO this should only be accessible in the cancellable interface
    var isCancelled: Boolean = false
        private set

    internal fun cancelEvent() {
        isCancelled = true
    }

    /**
     * Marker for events that listeners may cancel via [cancel].
     * Applicable to both [SkyHanniEvent] and [AsyncSkyHanniEvent] subclasses.
     */
    interface Cancellable {
        fun cancel() {
            (this as AbstractSkyHanniEvent).cancelEvent()
        }
    }
}
