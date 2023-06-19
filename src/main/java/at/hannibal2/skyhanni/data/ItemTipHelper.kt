package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorGuiContainer
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemTipHelper {

//    private val lastColorCacheTime = HashMap<String, Long>()
//    private val cache = HashMap<String, String>()

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock || stack.stackSize != 1) return

//        val uuid = stacremovek.getLore().joinToString { ", " }
        val stackTip: String
//        if (lastColorCacheTime.getOrDefault(uuid, 0L) + 1000 > System.currentTimeMillis()) {
//            stackTip = cache[uuid]!!
//        } else {
            val itemTipEvent = RenderItemTipEvent(stack, mutableListOf())
            itemTipEvent.postAndCatch()

        if (itemTipEvent.renderObjects.isEmpty()) return

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        for (renderObject in itemTipEvent.renderObjects) {
            val fontRenderer = event.fontRenderer
            val text = renderObject.text
            val x = event.x + 17 - fontRenderer.getStringWidth(text) + renderObject.offsetX
            val y = event.y + 9 + renderObject.offsetY
            fontRenderer.drawStringWithShadow(text, x.toFloat(), y.toFloat(), 16777215)
        }

        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderInventoryItemOverlayPost(event: DrawScreenAfterEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val gui = Minecraft.getMinecraft().currentScreen
        if (gui !is GuiChest) return
        val chest = gui.inventorySlots as ContainerChest
        var inventoryName = chest.getInventoryName()

        val guiLeft = (gui as AccessorGuiContainer).guiLeft
        val guiTop = (gui as AccessorGuiContainer).guiTop
        val fontRenderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()
        for (slot in gui.inventorySlots.inventorySlots) {
            val stack = slot.stack ?: continue

            val itemTipEvent = RenderInventoryItemTipEvent(inventoryName, slot, stack)
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