package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.ConfigUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object RenderData {

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        if (!canRender()) return
        if (!SkyHanniDebugsAndTests.globalRender) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return

        GlStateManager.translate(0f, 0f, -3f)
        GuiRenderEvent.GuiOverlayRenderEvent().post()
        GlStateManager.translate(0f, 0f, 3f)
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!canRender()) return
        if (!SkyHanniDebugsAndTests.globalRender) return
        if (GuiEditManager.isInGui() || VisualWordGui.isInGui()) return
        val currentScreen = Minecraft.getMinecraft().currentScreen ?: return
        if (currentScreen !is GuiInventory && currentScreen !is GuiChest) return

        GlStateManager.pushMatrix()
        GlStateManager.enableDepth()

        if (GuiEditManager.isInGui()) {
            GlStateManager.translate(0f, 0f, -3f)
            GuiRenderEvent.GuiOverlayRenderEvent().post()
            GlStateManager.translate(0f, 0f, 3f)
        }

        GuiRenderEvent.ChestGuiOverlayRenderEvent().post()

        GlStateManager.popMatrix()
    }

    private fun canRender(): Boolean = Minecraft.getMinecraft()?.renderManager?.fontRenderer != null

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        SkyHanniRenderWorldEvent(event.partialTicks).post()
    }

    // TODO find better spot for this
    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(17, "chroma.chromaDirection") { element ->
            ConfigUtils.migrateIntToEnum(element, ChromaConfig.Direction::class.java)
        }
    }
}
