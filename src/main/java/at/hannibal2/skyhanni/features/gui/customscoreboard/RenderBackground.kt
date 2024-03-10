package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SpecialColour
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class RenderBackground {
    private val config get() = SkyHanniMod.feature.gui.customScoreboard
    private val backgroundConfig get() = config.backgroundConfig

    fun renderBackground() {
        val position = config.position
        val border = 5

        val x = position.getAbsX()
        val y = position.getAbsY()

        val elementWidth = position.getDummySize().x
        val elementHeight = position.getDummySize().y

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight

        // Update the position to the alignment options
        if (
            config.displayConfig.alignment.alignRight
            || config.displayConfig.alignment.alignCenterVertically
        ) {
            position.set(
                Position(
                    if (config.displayConfig.alignment.alignRight)
                        scaledWidth - elementWidth - (backgroundConfig.borderSize * 2)
                    else x,
                    if (config.displayConfig.alignment.alignCenterVertically)
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


        if (backgroundConfig.enabled) {
            if (backgroundConfig.useCustomBackgroundImage) {
                val textureLocation = ResourceLocation("skyhanni", "scoreboard.png")
                Minecraft.getMinecraft().textureManager.bindTexture(textureLocation)

                Utils.drawTexturedRect(
                    (x - backgroundConfig.borderSize).toFloat(),
                    (y - backgroundConfig.borderSize).toFloat(),
                    (elementWidth + backgroundConfig.borderSize * 3).toFloat(),
                    (elementHeight + border * 2).toFloat(),
                    GL11.GL_NEAREST
                )
            } else {
                RenderUtils.drawRoundRect(
                    x - backgroundConfig.borderSize,
                    y - backgroundConfig.borderSize,
                    elementWidth + backgroundConfig.borderSize * 3,
                    elementHeight + backgroundConfig.borderSize * 2,
                    SpecialColour.specialToChromaRGB(backgroundConfig.color),
                    backgroundConfig.roundedCornerSmoothness
                )
            }
        }

        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }
}
