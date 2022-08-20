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

        val map = mutableMapOf<ItemStack, Int>()
        val mapTime = mutableMapOf<ItemStack, Long>()

        var ItemStack.background: Int
            get() {
                if (System.currentTimeMillis() > mapTime.getOrDefault(this, 0) + 200) return -1
                return map.getOrDefault(this, -1)
            }
            set(value) {
                map[this] = value
                mapTime[this] = System.currentTimeMillis()
            }
    }


    @SubscribeEvent
    fun renderOverlayLol(event: RenderRealOverlayEvent) {
        val stack = event.stack
        if (LorenzUtils.inSkyblock) {
            if (stack != null) {
                val color = stack.background
                if (color != -1) {
                    GlStateManager.pushMatrix()
                    GlStateManager.translate(0f, 0f, 110 + Minecraft.getMinecraft().renderItem.zLevel)
                    val x = event.x
                    val y = event.y
                    Gui.drawRect(x, y, x + 16, y + 16, color)
                    GlStateManager.popMatrix()
                }
            }
        }
    }
}