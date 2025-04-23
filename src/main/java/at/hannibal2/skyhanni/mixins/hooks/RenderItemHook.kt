package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderGuiItemOverlayEvent
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

fun renderItemOverlayPost(
    context: DrawContext,
    stack: ItemStack?,
    xPosition: Int,
    yPosition: Int,
    text: String?,
) {
    if (!SkyHanniDebugsAndTests.globalRender) return
    GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost(
        context,
        stack,
        xPosition,
        yPosition,
        text
    ).post()
}

fun renderItemReturn(context: DrawContext, stack: ItemStack, x: Int, y: Int) {
    if (!SkyHanniDebugsAndTests.globalRender) return
    RenderGuiItemOverlayEvent(context, stack, x, y).post()
}
