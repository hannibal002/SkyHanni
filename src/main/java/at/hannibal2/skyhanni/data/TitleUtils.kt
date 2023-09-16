package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.moulberry.moulconfig.internal.TextRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration

class TitleUtils {

    companion object {
        private var display = ""
        private var endTime = SimpleTimeMark.farPast()
        private var heightModifier = 1.8

        fun sendTitle(text: String, duration: Duration, height: Double = 1.8) {
            display = "§f$text"
            endTime = SimpleTimeMark.now() + duration
            heightModifier = height
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        endTime = SimpleTimeMark.farPast()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (endTime.isInPast()) return

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / heightModifier).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(display, renderer, 0f, 0f, false, 75, 0)
        GlStateManager.popMatrix()
    }
}