package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.RenderRealOverlayEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class ItemRenderBackground {

    companion object {

        private val backgroundColour = TimeLimitedCache<ItemStack, Int>(60.milliseconds)
        private val borderLineColour = TimeLimitedCache<ItemStack, Int>(60.milliseconds)

        var ItemStack.background: Int
            get() {
                return backgroundColour.getOrNull(this) ?: -1
            }
            set(value) {
                backgroundColour.put(this, value)
            }

        var ItemStack.borderLine: Int
            get() {
                return borderLineColour.getOrNull(this) ?: -1
            }
            set(value) {
                borderLineColour.put(this, value)
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
