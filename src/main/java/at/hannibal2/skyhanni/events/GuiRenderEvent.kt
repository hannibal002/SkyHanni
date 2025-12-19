package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphics

open class GuiRenderEvent(context: GuiGraphics) : RenderingSkyHanniEvent(context) {

    /**
     * Renders only while inside an inventory
     * This event does not render on signs.
     * Use ScreenDrawnEvent instead.
     * Also, ensure you do not render with this event while in a sign, as it will override ScreenDrawnEvent.
     */
    class ChestGuiOverlayRenderEvent(context: GuiGraphics) : GuiRenderEvent(context)

    /**
     * Renders always, and while in an inventory it renders a bit darker, gray
     */
    class GuiOverlayRenderEvent(context: GuiGraphics) : GuiRenderEvent(context)

    /**
     * Renders as [GuiOverlayRenderEvent] if not inside an inventory and runs as [ChestGuiOverlayRenderEvent] when inside an inventory
     */
    class GuiOnTopRenderEvent(context: GuiGraphics) : RenderingSkyHanniEvent(context)
    // This is intentional not an [GuiRenderEvent] since it will cause double renders
}
