package at.hannibal2.skyhanni.api.event

import net.minecraft.client.gui.DrawContext

/**
 * Used if the event is related to GUI rendering, needs a context passed to it
 */
abstract class RenderingSkyHanniEvent(override val context: DrawContext) : SkyHanniEvent(), SkyHanniEvent.Rendering
