package at.hannibal2.hanni.api.event

import at.hannibal2.hanni.utils.compat.DrawContext

/**
 * Used if the event is related to GUI rendering, needs a context passed to it
 */
abstract class RenderingHanniEvent(override val context: DrawContext) : HanniEvent(), HanniEvent.Rendering
