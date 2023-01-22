package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemRenderBackground {

    companion object {

        private val backgroundColor = mutableMapOf<ItemStack, Int>()
        private val borderLineColor = mutableMapOf<ItemStack, Int>()
        private val mapTime = mutableMapOf<ItemStack, Long>()

        var ItemStack.background: Int
            get() {
                if (System.currentTimeMillis() > mapTime.getOrDefault(this, 0) + 60) return -1
                return backgroundColor.getOrDefault(this, -1)
            }
            set(value) {
                backgroundColor[this] = value
                mapTime[this] = System.currentTimeMillis()
            }

        var ItemStack.borderLine: Int
            get() {
                if (System.currentTimeMillis() > mapTime.getOrDefault(this, 0) + 60) return -1
                return borderLineColor.getOrDefault(this, -1)
            }
            set(value) {
                borderLineColor[this] = value
                mapTime[this] = System.currentTimeMillis()
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