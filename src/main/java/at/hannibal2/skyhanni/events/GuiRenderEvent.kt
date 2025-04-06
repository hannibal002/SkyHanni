package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

open class GuiRenderEvent : SkyHanniEvent() {
    // Renders only while inside an inventory
    // This event does not render on signs.
    // Use ScreenDrawnEvent instead.
    // Also, ensure you do not render with this event while in a sign, as it will override ScreenDrawnEvent.
    class ChestGuiOverlayRenderEvent : GuiRenderEvent()
    // Renders always, and while in an inventory it renders a bit darker, gray
    class GuiOverlayRenderEvent : GuiRenderEvent()
}
