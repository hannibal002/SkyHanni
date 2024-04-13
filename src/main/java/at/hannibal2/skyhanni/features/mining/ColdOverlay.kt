package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI.getCold
import at.hannibal2.skyhanni.data.MiningAPI.inGlaciteArea
import at.hannibal2.skyhanni.events.GuiRenderEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11


class ColdOverlay {

    private val config get() = SkyHanniMod.feature.mining.coldOverlay
    private fun isEnabled() = config.enabled && inGlaciteArea()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val cold = getCold() ?: return
        if (cold < config.coldThreshold) return

        val mc = Minecraft.getMinecraft()
        val textureLocation = ResourceLocation("skyhanni", "cold_overlay.png")
        mc.textureManager.bindTexture(textureLocation)

        val scaledResolution = ScaledResolution(mc)
        val screenWidth = scaledResolution.scaledWidth
        val screenHeight = scaledResolution.scaledHeight

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.color(1f, 1f, 1f, 1f)

        Utils.drawTexturedRect(
            0f,
            0f,
            screenWidth.toFloat(),
            screenHeight.toFloat(),
            GL11.GL_NEAREST
        )

        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }
}
