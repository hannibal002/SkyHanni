package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.alignmentConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.backgroundConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.config
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SpecialColour
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class RenderBackground {
    fun renderBackground() {
        val position = config.position
        val border = backgroundConfig.borderSize

        val x = position.getAbsX()
        val y = position.getAbsY()

        val elementWidth = position.getDummySize().x
        val elementHeight = position.getDummySize().y

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight

        // Update the position to the alignment options
        if (
            alignmentConfig.alignRight
            || alignmentConfig.alignCenterVertically
        ) {
            position.set(
                Position(
                    if (alignmentConfig.alignRight)
                        scaledWidth - elementWidth - (border * 2)
                    else x,
                    if (alignmentConfig.alignCenterVertically)
                        scaledHeight / 2 - elementHeight / 2
                    else y,
                    position.getScale(),
                    position.isCenter
                )
            )
        }

        if (GuiEditManager.isInGui()) return

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.color(1f, 1f, 1f, 1f)
        GL11.glDepthMask(false)

        if (backgroundConfig.enabled) {
            if (backgroundConfig.useCustomBackgroundImage) {
                val textureLocation = ResourceLocation("skyhanni", "scoreboard.png")
                Minecraft.getMinecraft().textureManager.bindTexture(textureLocation)

                Utils.drawTexturedRect(
                    (x - border).toFloat(),
                    (y - border).toFloat(),
                    (elementWidth + border * 3).toFloat(),
                    (elementHeight + border * 2).toFloat(),
                    GL11.GL_NEAREST
                )
            } else {
                RenderUtils.drawRoundRect(
                    x - border,
                    y - border,
                    elementWidth + border * 3,
                    elementHeight + border * 2,
                    SpecialColour.specialToChromaRGB(backgroundConfig.color),
                    backgroundConfig.roundedCornerSmoothness
                )
            }
        }
        GL11.glDepthMask(true)
        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }
}
