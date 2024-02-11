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

    class RenderBackgroundData {
        var color = -1
        var time = 0L
        var lineColor = -1
        var borderTime = 0L
    }

    companion object {

        private fun ItemStack.getData() = cachedData.renderBackground

        var ItemStack.background: Int
            get() {
                val data = getData()
                data.time
                if (System.currentTimeMillis() > data.time + 60) return -1
                return data.color
            }
            set(value) {
                val data = getData()
                data.color = value
                data.time = System.currentTimeMillis()
            }

        var ItemStack.borderLine: Int
            get() {
                val data = getData()
                if (System.currentTimeMillis() > data.lineColor + 60) return -1
                return data.lineColor
            }
            set(value) {
                val data = getData()
                data.lineColor = value
                data.borderTime = System.currentTimeMillis()
            }
    }

    @SubscribeEvent
    fun renderOverlayLol(event: RenderRealOverlayEvent) {
        val stack = event.stack
        if (!LorenzUtils.inSkyBlock) return
        if (stack == null) return

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
