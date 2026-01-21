package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

@SkyHanniModule
object RenderData {

    @JvmStatic
    fun postRenderOverlay(context: GuiGraphics) {
        if (GlobalRender.renderDisabled) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return
        val screen = Minecraft.getInstance().screen

        DrawContextUtils.setContext(context)
        DrawContextUtils.translated(z = -3) {
            renderOverlay(DrawContextUtils.drawContext, screen != null && screen !is ChatScreen)
        }
        DrawContextUtils.clearContext()
    }

    @HandleEvent
    fun onBackgroundDraw(event: DrawBackgroundEvent) {
        if (GlobalRender.renderDisabled) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return
        val currentScreen = Minecraft.getInstance().screen ?: return
        if (currentScreen !is InventoryScreen && currentScreen !is ContainerScreen) return

        DrawContextUtils.pushPop {
            if (GuiEditManager.isInGui()) {
                DrawContextUtils.translated(z = -3) {
                    renderOverlay(DrawContextUtils.drawContext, true)
                }
            }
        }

        GuiRenderEvent.ChestGuiOverlayRenderEvent(DrawContextUtils.drawContext).post()
        GuiRenderEvent.GuiOnTopRenderEvent(DrawContextUtils.drawContext).post()
    }

    var outsideInventory = false

    fun renderOverlay(context: GuiGraphics, inventoryPresent: Boolean = false) {
        outsideInventory = true
        GuiRenderEvent.GuiOverlayRenderEvent(context).post()
        if (!inventoryPresent) GuiRenderEvent.GuiOnTopRenderEvent(context).post()
        outsideInventory = false
    }
}
