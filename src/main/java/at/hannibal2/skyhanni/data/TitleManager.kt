package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.moulberry.moulconfig.internal.TextRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TitleManager {

    companion object {
        private var display = ""
        private var endTime = SimpleTimeMark.farPast()
        private var heightModifier = 1.8
        private var fontSizeModifier = 4f

        fun sendTitle(text: String, duration: Duration, height: Double, fontSize: Float) {
            display = "§f$text"
            endTime = SimpleTimeMark.now() + duration
            heightModifier = height
            fontSizeModifier = fontSize
        }

        fun command(args: Array<String>) {
            if (args.size < 4) {
                LorenzUtils.userError("Usage: /shsendtitle <duration> <height> <fontSize> <text ..>")
                return
            }

            val duration = args[0].toInt().seconds
            val height = args[1].toDouble()
            val fontSize = args[2].toFloat()
            val title = "§6" + args.drop(3).joinToString(" ").replace("&", "§")

            sendTitle(title, duration, height, fontSize)
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        endTime = SimpleTimeMark.farPast()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (endTime.isInPast()) return

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / heightModifier).toFloat(), 0.0f)
        GlStateManager.scale(fontSizeModifier, fontSizeModifier, fontSizeModifier)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(display, renderer, 0f, 0f, true, 75, 0)
        GlStateManager.popMatrix()
    }
}
