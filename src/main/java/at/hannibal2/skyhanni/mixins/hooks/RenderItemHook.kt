package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderGuiItemOverlayEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

fun renderItemOverlayPost(
    context: GuiGraphics,
    stack: ItemStack?,
    xPosition: Int,
    yPosition: Int,
    text: String?,
) {
    if (GlobalRender.renderDisabled) return
    GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost(
        context,
        stack,
        xPosition,
        yPosition,
        text
    ).post()
}

fun renderItemReturn(context: GuiGraphics, stack: ItemStack, x: Int, y: Int) {
    if (GlobalRender.renderDisabled) return
    RenderGuiItemOverlayEvent(context, stack, x, y).post()
}
