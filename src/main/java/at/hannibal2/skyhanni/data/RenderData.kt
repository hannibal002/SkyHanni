package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.gui.DrawContext
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import at.hannibal2.skyhanni.utils.render.ModernGlStateManager

@SkyHanniModule
object RenderData {

    @JvmStatic
    fun postRenderOverlay(context: DrawContext) {
        if (GlobalRender.renderDisabled) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return
        val screen = MinecraftClient.getInstance().currentScreen

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
        val currentScreen = MinecraftClient.getInstance().currentScreen ?: return
        if (currentScreen !is InventoryScreen && currentScreen !is GenericContainerScreen) return

        DrawContextUtils.pushPop {
            ModernGlStateManager.enableDepthTest()

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

    fun renderOverlay(context: DrawContext, inventoryPresent: Boolean = false) {
        outsideInventory = true
        GuiRenderEvent.GuiOverlayRenderEvent(context).post()
        if (!inventoryPresent) GuiRenderEvent.GuiOnTopRenderEvent(context).post()
        outsideInventory = false
    }
}
