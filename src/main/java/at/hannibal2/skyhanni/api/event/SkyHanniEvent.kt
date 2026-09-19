package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Use @[HandleEvent]
 */
abstract class SkyHanniEvent protected constructor() {
    // TODO: This should only be accessible in the cancellable interface
    var isCancelled: Boolean = false
        private set

    fun post() = prePost(onError = null)

    fun post(onError: (Throwable) -> Unit = {}) = prePost(onError)

    private fun prePost(onError: ((Throwable) -> Unit)?): SkyHanniEvent = apply {
        (this as? Rendering)?.let { DrawContextUtils.setContext(it.context) }
        SkyHanniEvents.getEventHandler(javaClass).post(this, onError)
        if (this is Rendering) DrawContextUtils.clearContext()
    }

    interface Cancellable {
        fun cancel() {
            val event = this as SkyHanniEvent
            event.isCancelled = true
        }
    }

    interface Rendering {
        val context: GuiGraphicsExtractor
    }
}
