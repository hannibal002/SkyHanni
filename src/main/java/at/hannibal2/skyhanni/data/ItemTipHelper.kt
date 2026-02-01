package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.mixins.transformers.gui.AccessorHandledScreen
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.container
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen

@SkyHanniModule
object ItemTipHelper {

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return

        val itemTipEvent = RenderItemTipEvent(stack, mutableListOf())
        itemTipEvent.post()

        if (itemTipEvent.renderObjects.isEmpty()) return

        for (renderObject in itemTipEvent.renderObjects) {
            val text = renderObject.text
            val x = event.x + 17 + renderObject.offsetX
            val y = event.y + 9 + renderObject.offsetY

            event.drawSlotText(x, y, text, 1f)
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST, onlyOnSkyblock = true)
    fun onRenderInventoryItemOverlayPost(event: DrawScreenAfterEvent) {
        if (GlobalRender.renderDisabled) return

        val gui = Minecraft.getInstance().screen
        if (gui !is ContainerScreen) return
        val inventoryName = InventoryUtils.openInventoryName()

        val guiLeft = (gui as AccessorHandledScreen).guiLeft
        val guiTop = (gui as AccessorHandledScreen).guiTop
        val fontRenderer = Minecraft.getInstance().font

        DrawContextUtils.pushMatrix()
        for (slot in gui.container.slots) {
            val stack = slot.item.orNull() ?: continue

            val itemTipEvent = RenderInventoryItemTipEvent(inventoryName, slot, stack)
            itemTipEvent.post()
            val stackTip = itemTipEvent.stackTip
            if (stackTip.isEmpty()) continue

            val xDisplayPosition = slot.x
            val yDisplayPosition = slot.y

            val x = guiLeft + xDisplayPosition + 17 + itemTipEvent.offsetX - if (itemTipEvent.alignLeft) {
                fontRenderer.width(stackTip)
            } else 0
            val y = guiTop + yDisplayPosition + 9 + itemTipEvent.offsetY

            GuiRenderUtils.drawString(stackTip, x, y, -1)
        }
        DrawContextUtils.popMatrix()
    }
}
