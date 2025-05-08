package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

abstract class GuiRenderItemEvent(context: DrawContext) : RenderingSkyHanniEvent(context) {
    abstract class RenderOverlayEvent(
        context: DrawContext,
        open val stack: ItemStack?,
        open val x: Int,
        open val y: Int,
        open val text: String?,
    ) : GuiRenderItemEvent(context) {

        data class GuiRenderItemPost(
            override val context: DrawContext,
            override val stack: ItemStack?,
            override val x: Int,
            override val y: Int,
            override val text: String?,
        ) : RenderOverlayEvent(context, stack, x, y, text)
    }
}
