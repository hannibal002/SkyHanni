package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RenderGuiData {

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        if (!SkyHanniDebugsAndTests.globalRenderToggle) return
        if (GuiEditManager.isInGui() || FFGuideGUI.isInGui()) return

        GuiRenderEvent.GuiOverlayRenderEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!SkyHanniDebugsAndTests.globalRenderToggle) return
        if (GuiEditManager.isInGui() || FFGuideGUI.isInGui()) return
        val currentScreen = Minecraft.getMinecraft().currentScreen ?: return
        if (currentScreen !is GuiInventory && currentScreen !is GuiChest) return

        GlStateManager.pushMatrix()
        GlStateManager.enableDepth()

        if (GuiEditManager.isInGui()) {
            GuiRenderEvent.GuiOverlayRenderEvent().postAndCatch()
        }

        GuiRenderEvent.ChestGuiOverlayRenderEvent().postAndCatch()

        GlStateManager.popMatrix()
    }
}