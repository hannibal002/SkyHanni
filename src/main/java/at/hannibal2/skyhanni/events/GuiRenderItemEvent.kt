package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class GuiRenderItemEvent(context: GuiGraphicsExtractor) : RenderingSkyHanniEvent(context) {
    abstract class RenderOverlayEvent(
        context: GuiGraphicsExtractor,
        open val stack: SafeItemStack?,
        open val x: Int,
        open val y: Int,
        open val text: String?,
    ) : GuiRenderItemEvent(context) {

        data class GuiRenderItemPost(
            override val context: GuiGraphicsExtractor,
            override val stack: SafeItemStack?,
            override val x: Int,
            override val y: Int,
            override val text: String?,
        ) : RenderOverlayEvent(context, stack, x, y, text)
    }
}
