package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Base class for synchronous SkyHanni events.
 * Use @[HandleEvent] on a `private fun` to register a listener for an event of this type.
 */
abstract class SkyHanniEvent protected constructor() : AbstractSkyHanniEvent() {
    fun post() = prePost(onError = null)

    fun post(onError: (Throwable) -> Unit = {}) = prePost(onError)

    private fun prePost(onError: ((Throwable) -> Unit)?): SkyHanniEvent = apply {
        (this as? Rendering)?.let { DrawContextUtils.setContext(it.context) }
        SkyHanniEvents.getEventHandler(javaClass).post(this, onError)
        if (this is Rendering) DrawContextUtils.clearContext()
    }

    interface Rendering {
        val context: GuiGraphicsExtractor
    }
}
