package at.hannibal2.skyhanni.events

open class GuiRenderEvent : LorenzEvent() {
    class ChestGUIOverlayRenderEvent : GuiRenderEvent()
    class GUIOverlayRenderEvent : GuiRenderEvent()
}