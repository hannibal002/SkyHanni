package at.hannibal2.skyhanni.events

import net.minecraft.client.gui.FontRenderer
import net.minecraft.item.ItemStack

abstract class GuiRenderItemEvent : LorenzEvent() {
    abstract class RenderOverlayEvent(
        open val fontRenderer: FontRenderer,
        open val stack: ItemStack?,
        open val x: Int,
        open val y: Int,
        open val text: String?
    ) : GuiRenderItemEvent() {
        data class Post(
            override val fontRenderer: FontRenderer,
            override val stack: ItemStack?,
            override val x: Int,
            override val y: Int,
            override val text: String?
        ) :
            RenderOverlayEvent(fontRenderer, stack, x, y, text)
    }
}