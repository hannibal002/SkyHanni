package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

open class GuiRenderEvent : SkyHanniEvent() {
    class ChestGuiOverlayRenderEvent : GuiRenderEvent()
    class GuiOverlayRenderEvent : GuiRenderEvent()
}
