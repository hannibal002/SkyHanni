package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import net.minecraft.client.gui.FontRenderer
import net.minecraft.item.ItemStack

fun renderItemOverlayPost(
    fr: FontRenderer,
    stack: ItemStack?,
    xPosition: Int,
    yPosition: Int,
    text: String?,
) {
    if (!SkyHanniDebugsAndTests.globalRender) return
    GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost(
        fr,
        stack,
        xPosition,
        yPosition,
        text
    ).postAndCatch()
}

fun renderItemReturn(stack: ItemStack, x: Int, y: Int) {
    if (!SkyHanniDebugsAndTests.globalRender) return
    RenderRealOverlayEvent(stack, x, y).postAndCatch()
}
