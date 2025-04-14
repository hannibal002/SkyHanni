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

    fun post() = prePost(onError = null)

    fun post(onError: (Throwable) -> Unit = {}) = prePost(onError)

    private fun prePost(onError: ((Throwable) -> Unit)?): Boolean {
        if (this is Rendering) {
            DrawContextUtils.setContext(this.context)
            val result = SkyHanniEvents.getEventHandler(javaClass).post(this, onError)
            DrawContextUtils.clearContext()
            return result
        }
        return SkyHanniEvents.getEventHandler(javaClass).post(this, onError)
    }

    interface Cancellable {
        fun cancel() {
            val event = this as SkyHanniEvent
            event.isCancelled = true
        }
    }

    interface Rendering {
        val context: DrawContext
    }
}
