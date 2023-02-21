package at.hannibal2.skyhanni.events

class GuiRenderEvent(val type: RenderType): LorenzEvent() {

    enum class RenderType {
        INVENTORY_BACKGROUND,
        IN_WORLD,
        ;
    }
}