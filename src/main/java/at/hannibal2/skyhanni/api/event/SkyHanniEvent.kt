package at.hannibal2.skyhanni.api.event

/**
 * Use @[HandleEvent]
 */
abstract class SkyHanniEvent protected constructor() {

    // TODO: This should only be accessible in the cancellable interface
    // im not sure if i like this, since in the EventHandler we use the value of isCancelled regardless of if it is
    // a Cancellable event or not
    var isCancelled: Boolean = false
        private set

    protected fun post(context: Any? = null, onError: ((Throwable) -> Unit)? = null) =
        SkyHanniEvents.getEventHandler(javaClass).post(this, context, onError)

    // Events that want to add something to the context of the post event should override this method
    // and call the protected post function instead. See WidgetUpdateEvent for an example.
    open fun post() = post(null, null)

    // If events that add something to the context of the post also want to be able to post with an onError lambda, they should
    // also override this method.
    open fun post(onError: (Throwable) -> Unit) = post(null, onError)

    interface Cancellable {

        fun cancel() {
            val event = this as SkyHanniEvent
            event.isCancelled = true
        }
    }
}
