package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsY
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation

object RenderBackground {

    private val textureLocation by lazy { ResourceLocation("skyhanni", "scoreboard.png") }

    internal fun addBackground(renderable: Renderable): Renderable {
        val backgroundConfig = CustomScoreboard.backgroundConfig
        val outlineConfig = backgroundConfig.outline
        val padding = backgroundConfig.borderSize

        if (!backgroundConfig.enabled) return renderable

        val backgroundRenderable = if (backgroundConfig.useCustomBackgroundImage) {
            Renderable.drawInsideImage(
                renderable,
                textureLocation,
                (backgroundConfig.customBackgroundImageOpacity * 255) / 100,
                padding,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                radius = backgroundConfig.roundedCornerSmoothness,
            )
        } else {
            Renderable.drawInsideRoundedRect(
                renderable,
                backgroundConfig.color.toChromaColor(),
                padding,
                backgroundConfig.roundedCornerSmoothness,
                1,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        }

        return if (outlineConfig.enabled) {
            Renderable.drawInsideRoundedRectOutline(
                backgroundRenderable,
                0,
                backgroundConfig.roundedCornerSmoothness,
                1,
                outlineConfig.colorTop.toChromaColor().rgb,
                outlineConfig.colorBottom.toChromaColor().rgb,
                outlineConfig.thickness,
                outlineConfig.blur,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )
        } else backgroundRenderable
    }

    internal fun updatePosition(renderable: Renderable) {
        if (GuiEditManager.isInGui()) return
        val alignmentConfig = CustomScoreboard.alignmentConfig

        with(alignmentConfig) {
            if (horizontalAlignment == RenderUtils.HorizontalAlignment.DONT_ALIGN &&
                verticalAlignment == RenderUtils.VerticalAlignment.DONT_ALIGN
            ) return
        }

        val position = CustomScoreboard.config.position

        val scaledWidth = ScaledResolution(Minecraft.getMinecraft()).scaledWidth
        val scaledHeight = ScaledResolution(Minecraft.getMinecraft()).scaledHeight
        val elementWidth = (renderable.width * position.scale).toInt()
        val elementHeight = (renderable.height * position.scale).toInt()

        with(alignmentConfig) {
            var x = when (horizontalAlignment) {
                RenderUtils.HorizontalAlignment.DONT_ALIGN -> position.getAbsX()
                RenderUtils.HorizontalAlignment.LEFT -> 0 + margin
                RenderUtils.HorizontalAlignment.CENTER -> scaledWidth / 2 - elementWidth / 2
                RenderUtils.HorizontalAlignment.RIGHT -> scaledWidth - elementWidth - margin
                else -> 0
            }
            var y = when (verticalAlignment) {
                RenderUtils.VerticalAlignment.DONT_ALIGN -> position.getAbsY()
                RenderUtils.VerticalAlignment.TOP -> 0 + margin
                RenderUtils.VerticalAlignment.CENTER -> scaledHeight / 2 - elementHeight / 2
                RenderUtils.VerticalAlignment.BOTTOM -> scaledHeight - elementHeight - margin
                else -> 0
            }

            val outlineConfig = CustomScoreboard.backgroundConfig.outline
            if (outlineConfig.enabled) {
                val thickness = outlineConfig.thickness

                if (alignmentConfig.horizontalAlignment == RenderUtils.HorizontalAlignment.RIGHT) {
                    x -= thickness / 2
                } else if (alignmentConfig.horizontalAlignment == RenderUtils.HorizontalAlignment.LEFT) {
                    x += thickness / 2
                }

                if (alignmentConfig.verticalAlignment == RenderUtils.VerticalAlignment.TOP) {
                    y += thickness / 2
                } else if (alignmentConfig.verticalAlignment == RenderUtils.VerticalAlignment.BOTTOM) {
                    y -= thickness / 2
                }
            }
            CustomScoreboard.config.position.moveTo(x, y)
        }
    }
}
