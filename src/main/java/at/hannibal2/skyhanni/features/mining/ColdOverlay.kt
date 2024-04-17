package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI.inColdIsland
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import kotlin.time.Duration.Companion.seconds

class ColdOverlay {

    private val config get() = SkyHanniMod.feature.mining.coldOverlay
    var cold = 0
    var lastCold = 0
    private var lastColdUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val alpha = getColdAlpha()
        if (alpha == 0f) return

        val mc = Minecraft.getMinecraft()
        val textureLocation = ResourceLocation("skyhanni", "cold_overlay.png")
        mc.textureManager.bindTexture(textureLocation)

        val scaledResolution = ScaledResolution(mc)
        val screenWidth = scaledResolution.scaledWidth
        val screenHeight = scaledResolution.scaledHeight

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GL11.glDepthMask(false)
        GlStateManager.translate(0f, 0f, -500f)
        GlStateManager.color(1f, 1f, 1f, alpha)

        Utils.drawTexturedRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), GL11.GL_NEAREST)
        GL11.glDepthMask(true)

        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }

    private fun getColdAlpha(): Float {
        val coldInterp = NumberUtil.interpolate(cold.toFloat(), lastCold.toFloat(), lastColdUpdate.toMillis())
        val coldPercentage = (coldInterp - config.coldThreshold) / (100 - config.coldThreshold)
        return coldPercentage.coerceAtLeast(0f) * (config.maxAlpha / 100)
    }

    @SubscribeEvent
    fun onColdUpdate(event: ColdUpdateEvent) {
        val duration = if (event.cold == 0) 2.seconds else 0.seconds
        DelayedRun.runDelayed(duration) {
            lastCold = cold
            cold = event.cold
            lastColdUpdate = SimpleTimeMark.now()

        }
    }

    private fun isEnabled() = inColdIsland() && config.enabled
}
