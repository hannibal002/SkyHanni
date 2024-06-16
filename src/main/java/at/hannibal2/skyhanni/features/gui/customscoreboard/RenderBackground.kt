package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.getDummySize
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.RenderUtils
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class RenderBackground {

    fun renderBackground() {
        val alignmentConfig = CustomScoreboard.alignmentConfig
        val backgroundConfig = CustomScoreboard.backgroundConfig
        val outlineConfig = backgroundConfig.outline
        val position = CustomScoreboard.config.position
        val border = backgroundConfig.borderSize

        val x = position.getAbsX()
        val y = position.getAbsY()

        val elementWidth = position.getDummySize().x
        val elementHeight = position.getDummySize().y

        // Update the position to the alignment options
        if (
            alignmentConfig.horizontalAlignment != RenderUtils.HorizontalAlignment.DONT_ALIGN
            || alignmentConfig.verticalAlignment != RenderUtils.VerticalAlignment.DONT_ALIGN
        ) {
            position.set(updatePosition(position))
        }

        if (GuiEditManager.isInGui()) return

        GlStateManager.pushMatrix()

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
                    backgroundConfig.color.toChromaColor().rgb,
                    backgroundConfig.roundedCornerSmoothness
                )
            }
            if (outlineConfig.enabled) {
                RenderUtils.drawRoundRectOutline(
                    x - border,
                    y - border,
                    elementWidth + border * 3,
                    elementHeight + border * 2,
                    outlineConfig.colorTop.toChromaColor().rgb,
                    outlineConfig.colorBottom.toChromaColor().rgb,
                    outlineConfig.thickness,
                    backgroundConfig.roundedCornerSmoothness,
                    outlineConfig.blur
                )
            }
        }
        GL11.glDepthMask(true)
        GlStateManager.popMatrix()
    }

    private fun updatePosition(position: Position): Position {
        val alignmentConfig = CustomScoreboard.alignmentConfig
        val backgroundConfig = CustomScoreboard.backgroundConfig
        val outlineConfig = backgroundConfig.outline
        val border = backgroundConfig.borderSize

        val x = position.getAbsX()
        val y = position.getAbsY()

        val elementWidth = position.getDummySize().x
        val elementHeight = position.getDummySize().y

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight


        var newX = when (alignmentConfig.horizontalAlignment) {
            RenderUtils.HorizontalAlignment.LEFT -> border
            RenderUtils.HorizontalAlignment.CENTER -> scaledWidth / 2 - (elementWidth + border * 3) / 2
            RenderUtils.HorizontalAlignment.RIGHT -> scaledWidth - (elementWidth + border * 2)
            else -> x
        }

        var newY = when (alignmentConfig.verticalAlignment) {
            RenderUtils.VerticalAlignment.TOP -> border
            RenderUtils.VerticalAlignment.CENTER -> scaledHeight / 2 - (elementHeight + border * 2) / 2
            RenderUtils.VerticalAlignment.BOTTOM -> scaledHeight - elementHeight - border
            else -> y
        }

        if (outlineConfig.enabled) {
            val thickness = outlineConfig.thickness
            if (alignmentConfig.horizontalAlignment == RenderUtils.HorizontalAlignment.RIGHT) {
                newX -= thickness / 2
            } else if (alignmentConfig.horizontalAlignment == RenderUtils.HorizontalAlignment.LEFT) {
                newX += thickness / 2
            }

            if (alignmentConfig.verticalAlignment == RenderUtils.VerticalAlignment.TOP) {
                newY += thickness / 2
            } else if (alignmentConfig.verticalAlignment == RenderUtils.VerticalAlignment.BOTTOM) {
                newY -= thickness / 2
            }
        }

        return Position(
            newX,
            newY,
            position.getScale(),
            position.isCenter
        )
    }
}
