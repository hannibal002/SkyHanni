package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiRenderEvent
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

        GuiRenderEvent.GameOverlayRenderEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        val currentScreen = Minecraft.getMinecraft().currentScreen ?: return

        if (currentScreen !is GuiInventory && currentScreen !is GuiChest) return

        GlStateManager.pushMatrix()
        GlStateManager.enableDepth()

        GuiRenderEvent.ChestBackgroundRenderEvent().postAndCatch()

        GlStateManager.popMatrix()
    }
}