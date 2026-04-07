package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.client.gui.GuiGraphics

abstract class GuiRenderItemEvent(context: GuiGraphics) : RenderingSkyHanniEvent(context) {
    abstract class RenderOverlayEvent(
        context: GuiGraphics,
        open val stack: SafeItemStack?,
        open val x: Int,
        open val y: Int,
        open val text: String?,
    ) : GuiRenderItemEvent(context) {

        data class GuiRenderItemPost(
            override val context: GuiGraphics,
            override val stack: SafeItemStack?,
            override val x: Int,
            override val y: Int,
            override val text: String?,
        ) : RenderOverlayEvent(context, stack, x, y, text)
    }
}
