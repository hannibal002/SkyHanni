package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SendTitleHelper {

    companion object {
        private var textToRender = ""
        private var endTime = 0L

        fun sendTitle(text: String, duration: Int) {
            textToRender = text
            endTime = System.currentTimeMillis() + duration
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return

        if (System.currentTimeMillis() > endTime) return

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / 1.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textToRender, renderer, 0f, 0f, false, 75, 0)
        GlStateManager.popMatrix()
    }
}