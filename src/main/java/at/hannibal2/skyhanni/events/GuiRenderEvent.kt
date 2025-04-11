package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext

open class GuiRenderEvent(open val context: DrawContext) : SkyHanniEvent() {
    // Renders only while inside an inventory
    // This event does not render on signs.
    // Use ScreenDrawnEvent instead.
    // Also, ensure you do not render with this event while in a sign, as it will override ScreenDrawnEvent.
    class ChestGuiOverlayRenderEvent(override val context: DrawContext) : GuiRenderEvent(context)
    // Renders always, and while in an inventory it renders a bit darker, gray
    class GuiOverlayRenderEvent(override val context: DrawContext) : GuiRenderEvent(context)
}
