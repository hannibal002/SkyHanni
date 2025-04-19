package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

abstract class GuiRenderItemEvent : SkyHanniEvent() {
    abstract class RenderOverlayEvent(
        open val context: DrawContext,
        open val stack: ItemStack?,
        open val x: Int,
        open val y: Int,
        open val text: String?,
    ) : GuiRenderItemEvent() {

        data class GuiRenderItemPost(
            override val context: DrawContext,
            override val stack: ItemStack?,
            override val x: Int,
            override val y: Int,
            override val text: String?,
        ) : RenderOverlayEvent(context, stack, x, y, text)
    }
}
