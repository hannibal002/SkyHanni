package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent

@SkyHanniModule
object RenderData {

    @HandleEvent
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        if (!SkyHanniDebugsAndTests.globalRender) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return

        DrawContextUtils.translate(0f, 0f, -3f)
        renderOverlay(DrawContextUtils.drawContext)
        DrawContextUtils.translate(0f, 0f, 3f)
    }

    @HandleEvent
    fun onBackgroundDraw(event: DrawBackgroundEvent) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return
        val currentScreen = Minecraft.getMinecraft().currentScreen ?: return
        if (currentScreen !is GuiInventory && currentScreen !is GuiChest) return

        DrawContextUtils.pushMatrix()
        GlStateManager.enableDepth()

        if (GuiEditManager.isInGui()) {
            DrawContextUtils.translate(0f, 0f, -3f)
            renderOverlay(DrawContextUtils.drawContext)
            DrawContextUtils.translate(0f, 0f, 3f)
        }

        GuiRenderEvent.ChestGuiOverlayRenderEvent(DrawContextUtils.drawContext).post()

        DrawContextUtils.popMatrix()
    }

    var outsideInventory = false

    fun renderOverlay(context: DrawContext) {
        outsideInventory = true
        GuiRenderEvent.GuiOverlayRenderEvent(context).post()
        outsideInventory = false
    }
}
