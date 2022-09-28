package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemTipHelper {

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.Post) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyblock || stack.stackSize != 1) return

        val itemTipEvent = RenderItemTipEvent(stack)
        itemTipEvent.postAndCatch()
        val stackTip = itemTipEvent.stackTip
        if (stackTip.isEmpty()) return

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        val x = event.x + 17 + itemTipEvent.offsetX - if (itemTipEvent.alignLeft) {
            event.fontRenderer.getStringWidth(stackTip)
        } else 0
        val y = event.y + 9 + itemTipEvent.offsetY

        event.fontRenderer.drawStringWithShadow(stackTip, x.toFloat(), y.toFloat(), 16777215)
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }
}