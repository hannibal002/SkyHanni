package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.cachedData
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemRenderBackground {

    companion object {

        var ItemStack.background: Int
            get() = cachedData.itemBackground
            set(value) {
                cachedData.itemBackground = value
            }

        var ItemStack.borderLine: Int
            get() = cachedData.itemBorderLine
            set(value) {
                cachedData.itemBorderLine = value
            }
    }

    @SubscribeEvent
    fun onRenderRealOverlay(event: RenderRealOverlayEvent) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock) return

        val backgroundColor = stack.background
        if (backgroundColor != -1) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(0f, 0f, 110 + Minecraft.getMinecraft().renderItem.zLevel)
            val x = event.x
            val y = event.y
            Gui.drawRect(x, y, x + 16, y + 16, backgroundColor)
            GlStateManager.popMatrix()
        }

        val borderLineColor = stack.borderLine
        if (borderLineColor != -1) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(0f, 0f, 110 + Minecraft.getMinecraft().renderItem.zLevel)
            val x = event.x
            val y = event.y

            Gui.drawRect(x, y, x + 1, y + 16, borderLineColor)
            Gui.drawRect(x, y, x + 16, y + 1, borderLineColor)

            Gui.drawRect(x, y + 15, x + 16, y + 16, borderLineColor)
            Gui.drawRect(x + 15, y, x + 16, y + 16, borderLineColor)
            GlStateManager.popMatrix()
        }
    }
}
