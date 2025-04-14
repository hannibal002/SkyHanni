package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils

/**
 * Use @[HandleEvent]
 */
abstract class SkyHanniEvent protected constructor() {
    // TODO: This should only be accessible in the cancellable interface
    // im not sure if i like this, since in the EventHandler we use the value of isCancelled regardless of if it is
    // a Cancellable event or not
    var isCancelled: Boolean = false
        private set

    // Events that want to add something to the context of the post event should override this method
    // and call the protected post function instead. See WidgetUpdateEvent for an example.
    open fun post() = post(null, null)

    // If events that add something to the context of the post also want to be able to post with
    // an onError lambda, they should also override this method.
    open fun post(onError: (Throwable) -> Unit = {}) = post(null, onError)

    protected fun post(context: Any? = null, onError: ((Throwable) -> Unit)? = null): Boolean {
        if (this is Rendering) {
            DrawContextUtils.setContext(this.drawContext)
            val result = SkyHanniEvents.getEventHandler(javaClass).post(this, context, onError)
            DrawContextUtils.clearContext()
            return result
        }
        return SkyHanniEvents.getEventHandler(javaClass).post(this, context, onError)
    }

    interface Cancellable {
        fun cancel() {
            val event = this as SkyHanniEvent
            event.isCancelled = true
        }
    }

    interface Rendering {
        val drawContext: DrawContext
    }
}
