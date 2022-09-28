package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.EventPriority
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

        val fontRenderer = event.fontRenderer
        val x = event.x + 17 - fontRenderer.getStringWidth(stackTip)
        val y = event.y + 9

        fontRenderer.drawStringWithShadow(stackTip, x.toFloat(), y.toFloat(), 16777215)
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderInventoryItemOverlayPost(event: DrawScreenAfterEvent) {
        if (!LorenzUtils.inSkyblock) return

        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is GuiChest) return

        val guiLeft = (gui as AccessorGuiContainer).guiLeft
        val guiTop = (gui as AccessorGuiContainer).guiTop
        val fontRenderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()
        for (slot in gui.inventorySlots.inventorySlots) {
            val stack = slot.stack ?: continue
            if (stack.stackSize != 1) continue

            val itemTipEvent = RenderInventoryItemTipEvent(stack)
            itemTipEvent.postAndCatch()
            val stackTip = itemTipEvent.stackTip
            if (stackTip.isEmpty()) continue

            val xDisplayPosition = slot.xDisplayPosition
            val yDisplayPosition = slot.yDisplayPosition

            val x = guiLeft + xDisplayPosition + 17 + itemTipEvent.offsetX - if (itemTipEvent.alignLeft) {
                fontRenderer.getStringWidth(stackTip)
            } else 0
            val y = guiTop + yDisplayPosition + 9 + itemTipEvent.offsetY

            fontRenderer.drawStringWithShadow(stackTip, x.toFloat(), y.toFloat(), 16777215)
        }
        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }
}