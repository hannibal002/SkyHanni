package at.hannibal2.skyhanni.api.event

/**
 * Base class for asynchronous SkyHanni events.
 * Use @[HandleEvent] on a `private suspend fun` to register a listener for an event of this type.
 */
abstract class AsyncSkyHanniEvent protected constructor() : AbstractSkyHanniEvent() {
    suspend fun post() = AsyncEventDispatchTracker.dispatch {
        SkyHanniEvents.getAsyncEventHandler(javaClass).post(this, onError = null)
    }

    suspend fun post(onError: (Throwable) -> Unit = {}) = AsyncEventDispatchTracker.dispatch {
        SkyHanniEvents.getAsyncEventHandler(javaClass).post(this, onError)
    }
}
