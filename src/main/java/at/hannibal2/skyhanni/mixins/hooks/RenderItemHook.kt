package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import net.minecraft.client.gui.FontRenderer
import net.minecraft.item.ItemStack

fun renderItemOverlayPost(
    fr: FontRenderer,
    stack: ItemStack?,
    xPosition: Int,
    yPosition: Int,
    text: String?,
) {
    GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost(
        fr,
        stack,
        xPosition,
        yPosition,
        text
    ).postAndCatch()
}

fun renderItemReturn(stack: ItemStack, x: Int, y: Int) {
    RenderRealOverlayEvent(stack, x, y).postAndCatch()
}
