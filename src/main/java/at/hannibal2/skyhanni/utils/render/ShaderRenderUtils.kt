package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.shader.CircleShader
import at.hannibal2.skyhanni.shader.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.shader.RoundedRectangleShader
import at.hannibal2.skyhanni.shader.RoundedShader
import at.hannibal2.skyhanni.shader.RoundedTextureShader
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.shader.ShaderManager
import net.minecraft.util.ResourceLocation
import java.awt.Color
import kotlin.math.max
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.utils.render.RoundedShapeDrawer
//$$ import org.joml.Matrix4f
//#endif

object ShaderRenderUtils {

    /**
     * Helper method to assist with setting up the shader for drawing rounded shapes.
     */
    private fun <T : RoundedShader<T>> T.applyBaseSettings(
        radius: Int,
        width: Int, height: Int, x: Int, y: Int,
        smoothness: Float = 0f,
        extraApplies: (T.() -> Unit)? = null,
    ) = this.apply {
        val scaleFactor = GuiScreenUtils.scaleFactor
        val widthIn = width * scaleFactor
        val heightIn = height * scaleFactor
        val xIn = x * scaleFactor
        val yIn = y * scaleFactor

        this.scaleFactor = scaleFactor.toFloat()
        this.radius = radius.toFloat()
        this.smoothness = smoothness
        this.halfSize = floatArrayOf(widthIn / 2f, heightIn / 2f)
        this.centerPos = floatArrayOf(xIn + (widthIn / 2f), yIn + (heightIn / 2f))
        //#if MC > 1.21
        //$$ this.modelViewMatrix = Matrix4f(DrawContextUtils.drawContext.matrices.peek().positionMatrix)
        //#endif
    }.also { extraApplies?.invoke(this) }

    /**
     * Method to draw a rounded textured rect.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the textured rect will not be rendered correctly
     *
     * @param filter the texture filter to use
     * @param radius the radius of the corners (default 10), NOTE: If you pass less than 1 it will just draw as a normal textured rect
     * @param smoothness how smooth the corners will appear (default 1). NOTE: This does very
     * little to the smoothness of the corners in reality due to how the final pixel color is calculated.
     * It is best kept at its default.
     */
    fun drawRoundTexturedRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        filter: Int,
        radius: Int = 10,
        smoothness: Float = 1f,
        texture: ResourceLocation,
        alpha: Float = 1f,
    ) {
        // if radius is 0 then just draw a normal textured rect
        if (radius <= 0) return GuiRenderUtils.drawTexturedRect(x, y, width, height, filter = filter, texture = texture, alpha = alpha)

        RoundedTextureShader.applyBaseSettings(radius, width, height, x, y, smoothness)

        //#if MC < 1.21
        DrawContextUtils.pushPop {
            ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_TEXTURE)
            GuiRenderUtils.drawTexturedRect(x, y, width, height, filter = filter, texture = texture, alpha = alpha)
            ShaderManager.disableShader()
        }
        //#else
        //$$ RoundedShapeDrawer.drawRoundedTexturedRect(x, y, width, height, texture)
        //#endif
    }

    /**
     * Method to draw a rounded rectangle.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param color color of rect
     * @param radius the radius of the corners (default 10)
     * @param smoothness how smooth the corners will appear (default 1). NOTE: This does very
     * little to the smoothness of the corners in reality due to how the final pixel color is calculated.
     * It is best kept at its default.
     */
    fun drawRoundRect(x: Int, y: Int, width: Int, height: Int, color: Int, radius: Int = 10, smoothness: Float = 1f) {
        RoundedRectangleShader.applyBaseSettings(radius, width, height, x, y, smoothness)

        //#if MC < 1.21
        DrawContextUtils.pushPop {
            ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_RECTANGLE)
            GuiRenderUtils.drawRect(x - 5, y - 5, x + width + 5, y + height + 5, color)
            ShaderManager.disableShader()
        }
        //#else
        //$$ RoundedShapeDrawer.drawRoundedRect(x - 5, y - 5, x + width + 5, y + height + 5, color)
        //#endif
    }

    /**
     * Method to draw the outline of a rounded rectangle with a color gradient. For a single color just pass
     * in the color to both topColor and bottomColor.
     *
     * This is *not* a method that draws a rounded rectangle **with** an outline, rather, this draws **only** the outline.
     *
     * **NOTE:** The same notices given from [drawRoundRect] should be acknowledged with this method also.
     *
     * @param topColor color of the top of the outline
     * @param bottomColor color of the bottom of the outline
     * @param borderThickness the thickness of the border
     * @param radius radius of the corners of the rectangle (default 10)
     * @param blur the amount to blur the outline (default 0.7f)
     */
    fun drawRoundRectOutline(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        topColor: Int,
        bottomColor: Int,
        borderThickness: Int,
        radius: Int = 10,
        blur: Float = 0.7f,
    ) {
        RoundedRectangleOutlineShader.applyBaseSettings(radius, width, height, x, y) {
            this.borderThickness = borderThickness.toFloat()
            // The blur argument is a bit misleading, the greater the value the more sharp the edges of the
            // outline will be and the smaller the value the blurrier. So we take the difference from 1
            // so the shader can blur the edges accordingly. This is because a 'blurriness' option makes more sense
            // to users than a 'sharpness' option in this context
            this.borderBlur = max(1 - blur, 0f)
        }

        val borderAdjustment = borderThickness / 2
        val left = x - borderAdjustment
        val top = y - borderAdjustment
        val right = x + width + borderAdjustment
        val bottom = y + height + borderAdjustment

        //#if MC < 1.21
        DrawContextUtils.pushPop {
            ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_RECT_OUTLINE)
            GuiRenderUtils.drawGradientRect(left, top, right, bottom, topColor, bottomColor)
            ShaderManager.disableShader()
        }
        //#else
        //$$ RoundedShapeDrawer.drawRoundedRectOutline(left, top, right, bottom, topColor, bottomColor)
        //#endif
    }

    /**
     * Method to draw a rounded rectangle.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param topColor the color of the top of the rectangle
     * @param bottomColor the color of the bottom of the rectangle
     * @param radius the radius of the corners (default 10)
     * @param smoothness how smooth the corners will appear (default 1). NOTE: This does very
     * little to the smoothness of the corners in reality due to how the final pixel color is calculated.
     * It is best kept at its default.
     */
    fun drawRoundGradientRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        topColor: Int,
        bottomColor: Int,
        radius: Int = 10,
        smoothness: Float = 1f,
    ) {
        RoundedRectangleShader.applyBaseSettings(radius, width, height, x, y, smoothness)

        val left = x - 5
        val top = y - 5
        val right = x + width + 5
        val bottom = y + height + 5

        //#if MC < 1.21
        DrawContextUtils.pushPop {
            ShaderManager.enableShader(ShaderManager.Shaders.ROUNDED_RECTANGLE)
            GuiRenderUtils.drawGradientRect(left, top, right, bottom, topColor, bottomColor)
            ShaderManager.disableShader()
        }
        //#else
        //$$ RoundedShapeDrawer.drawRoundedRect(left, top, right, bottom, topColor, bottomColor)
        //#endif
    }

    /**
     * Method to draw a circle.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param x The x-coordinate of the circle's center.
     * @param y The y-coordinate of the circle's center.
     * @param radius The circle's radius.
     * @param color The fill color.
     * @param angle1 defines the start of the semicircle (Default value makes it a full circle). Must be in range [0,2*pi] (0 is on the left and increases counterclockwise)
     * @param angle2 defines the end of the semicircle (Default value makes it a full circle). Must be in range [0,2*pi] (0 is on the left and increases counterclockwise)
     * @param smoothness smooths out the edge. (In amount of blurred pixels)
     */
    fun drawFilledCircle(
        x: Int,
        y: Int,
        color: Color,
        radius: Int = 10,
        smoothness: Float = 1f,
        angle1: Float = 7.0f,
        angle2: Float = 7.0f
    ) {
        val radiusIn = radius * GuiScreenUtils.scaleFactor
        val diameter = radius * 2

        CircleShader.applyBaseSettings(radiusIn, diameter, diameter, x, y, smoothness) {
            this.angle1 = angle1 - Math.PI.toFloat()
            this.angle2 = angle2 - Math.PI.toFloat()
        }

        // TODO: Once ChromaColour no longer drops alpha sometimes, remove this 255 hardcode
        val circleColor = color.addAlpha(255).rgb

        val left = x - 5
        val top = y - 5
        val right = x + (radius * 2) + 5
        val bottom = y + (radius * 2) + 5

        //#if MC < 1.21
        DrawContextUtils.pushPop {
            ShaderManager.enableShader(ShaderManager.Shaders.CIRCLE)
            GuiRenderUtils.drawRect(left, top, right, bottom, circleColor)
            ShaderManager.disableShader()
        }
        //#else
        //$$ RoundedShapeDrawer.drawCircle(left, top, right, bottom, circleColor)
        //#endif
    }
}
