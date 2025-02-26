package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

open class GuiRenderEvent : SkyHanniEvent() {
    /**
    Renders only while inside an inventory
     */
    object ChestGuiOverlayRenderEvent : GuiRenderEvent()

    /**
    Renders always, and while in an inventory it renders a bit darker, gray
     */
    object GuiOverlayRenderEvent : GuiRenderEvent()

    /**
     * Renders as [GuiOverlayRenderEvent] if not inside an inventory and runs as [ChestGuiOverlayRenderEvent] when inside an inventory
     */
    object GuiOnTopRenderEvent : SkyHanniEvent() // This is intentional not an [GuiRenderEvent] since it will cause double renders
}
