package at.hannibal2.skyhanni.events

open class GuiRenderEvent : LorenzEvent() {
    class ChestBackgroundRenderEvent : GuiRenderEvent()
    class GameOverlayRenderEvent : GuiRenderEvent()
}