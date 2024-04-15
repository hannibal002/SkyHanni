package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.MiningAPI.inColdIsland
import at.hannibal2.skyhanni.events.ColdUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.NumberUtil
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

class ColdOverlay {

    private val config get() = SkyHanniMod.feature.mining.coldOverlay
    var cold = 0
    var lastCold = 0

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
        val coldInterp = NumberUtil.interpolate(cold.toFloat(), lastCold.toFloat(), MiningAPI.lastColdUpdate.toMillis())
        val coldPercentage = (coldInterp - config.coldThreshold) / (100 - config.coldThreshold)
        return coldPercentage.coerceAtLeast(0f) * (config.maxAlpha / 100)
    }

    @SubscribeEvent
    fun onColdUpdate(event: ColdUpdateEvent) {
        lastCold = cold
        cold = event.cold
    }

    private fun isEnabled() = inColdIsland() && config.enabled
}
