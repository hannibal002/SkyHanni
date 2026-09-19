package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.client.gui.GuiGraphicsExtractor

@PrimaryFunction("onGuiRender")
sealed class GuiRenderEvent(context: GuiGraphicsExtractor) : RenderingSkyHanniEvent(context) {

    /**
     * Renders only while inside an inventory
     * This event does not render on signs.
     * Use ScreenDrawnEvent instead.
     * Also, ensure you do not render with this event while in a sign, as it will override ScreenDrawnEvent.
     */
    @PrimaryFunction("onChestGuiRender")
    class ChestGuiOverlayRenderEvent(context: GuiGraphicsExtractor) : GuiRenderEvent(context)

    /**
     * Renders always, and while in an inventory it renders a bit darker, gray
     */
    @PrimaryFunction("onGuiRenderOverlay")
    class GuiOverlayRenderEvent(context: GuiGraphicsExtractor) : GuiRenderEvent(context)

    /**
     * Renders as [GuiOverlayRenderEvent] if not inside an inventory and runs as [ChestGuiOverlayRenderEvent] when inside an inventory
     */
    @PrimaryFunction("onGuiRenderTop")
    class GuiOnTopRenderEvent(context: GuiGraphicsExtractor) : RenderingSkyHanniEvent(context)
    // This is intentional not an [GuiRenderEvent] since it will cause double renders
}
