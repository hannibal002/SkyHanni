package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.shader.CircleShader
import at.hannibal2.skyhanni.shader.RadialGradientCircleShader
import at.hannibal2.skyhanni.shader.RoundedRectangleOutlineShader
import at.hannibal2.skyhanni.shader.RoundedRectangleShader
import at.hannibal2.skyhanni.shader.RoundedShader
import at.hannibal2.skyhanni.shader.RoundedTextureShader
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.render.atlas.SkyHanniRoundedShapeAtlasKey
import at.hannibal2.skyhanni.utils.render.states.RoundedRenderStateParams
import at.hannibal2.skyhanni.utils.render.states.SkyHanniCircleRenderState
import at.hannibal2.skyhanni.utils.render.states.SkyHanniRadialGradientCircleRenderState
import at.hannibal2.skyhanni.utils.render.states.SkyHanniRoundedRectOutlineRenderState
import at.hannibal2.skyhanni.utils.render.states.SkyHanniRoundedRectRenderState
import at.hannibal2.skyhanni.utils.render.states.SkyHanniRoundedTexturedRectRenderState
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.resources.Identifier
import org.joml.Matrix3x2f
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.max
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
object ShaderRenderUtils {

    /**
     * Returns a float array representation of the [ChromaColour].
     */
    private fun ChromaColour.destructToFloatArray(): FloatArray = floatArrayOf(
        this.toColor().red.toFloat() / 255f,
        this.toColor().green.toFloat() / 255f,
        this.toColor().blue.toFloat() / 255f,
        this.alpha.toFloat() / 255f,
    )

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

        val matrix3x2f = Matrix3x2f(DrawContextUtils.drawContext.pose())
        this.modelViewMatrix = Matrix4f()
            .setTranslation(matrix3x2f.m20(), matrix3x2f.m21(), -11000.0f)
            .scale(matrix3x2f.m00(), matrix3x2f.m11(), 1.0f)
    }.also { extraApplies?.invoke(this) }

    /**
     * Method to draw a rounded textured rect.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the textured rect will not be rendered correctly
     *
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
        radius: Int = 10,
        smoothness: Float = 1f,
        texture: Identifier,
        alpha: Float = 1f,
    ) {
        if (radius <= 0) return GuiRenderUtils.drawTexturedRect(x, y, width, height, texture = texture, alpha = alpha)

        //? if >= 26.1 {
        drawRoundTexturedRectDeferred(x, y, width, height, radius, smoothness, texture, alpha)
        //?} else {
        /*RoundedTextureShader.applyBaseSettings(radius, width, height, x, y, smoothness)
        RoundedShapeDrawer.drawRoundedTexturedRect(x, y, width, height, texture)
        *///?}
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
        //? if >= 26.1 {
        drawRoundRectDeferred(x, y, width, height, color, radius, smoothness)
        //?} else {
        /*RoundedRectangleShader.applyBaseSettings(radius, width, height, x, y, smoothness)
        RoundedShapeDrawer.drawRoundedRect(x - 5, y - 5, x + width + 5, y + height + 5, color)
        *///?}
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
        //? if >= 26.1 {
        drawRoundRectOutlineDeferred(x, y, width, height, topColor, bottomColor, borderThickness, radius, blur)
        //?} else {
        /*RoundedRectangleOutlineShader.applyBaseSettings(radius, width, height, x, y) {
            this.borderThickness = borderThickness.toFloat()
            // The blur argument is a bit misleading, the greater the value the more sharp the edges of the
            // outline will be and the smaller the value the blurrier. So we take the difference from 1
            // so the shader can blur the edges accordingly. This is because a 'blurriness' option makes more sense
            // to users than a 'sharpness' option in this context
            this.borderBlur = max(1 - blur, 0f)
        }
        val borderAdjustment = borderThickness / 2
        RoundedShapeDrawer.drawRoundedRectOutline(
            x - borderAdjustment, y - borderAdjustment,
            x + width + borderAdjustment, y + height + borderAdjustment,
            topColor, bottomColor,
        )
        *///?}
    }

    /**
     * Method to draw a rounded rectangle with a vertical color gradient.
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
        //? if >= 26.1 {
        drawRoundGradientRectDeferred(x, y, width, height, topColor, bottomColor, radius, smoothness)
        //?} else {
        /*RoundedRectangleShader.applyBaseSettings(radius, width, height, x, y, smoothness)
        RoundedShapeDrawer.drawRoundedRect(x - 5, y - 5, x + width + 5, y + height + 5, topColor, bottomColor)
        *///?}
    }

    /**
     * Method to draw a circle.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param x The x-coordinate of the circle's top-left bounding box corner.
     * @param y The y-coordinate of the circle's top-left bounding box corner.
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
        angle2: Float = 7.0f,
    ) {
        //? if >= 26.1 {
        drawFilledCircleDeferred(x, y, color, radius, smoothness, angle1, angle2)
        //?} else {
        /*val radiusIn = radius * GuiScreenUtils.scaleFactor
        val diameter = radius * 2
        CircleShader.applyBaseSettings(radiusIn, diameter, diameter, x, y, smoothness) {
            this.angle1 = angle1 - Math.PI.toFloat()
            this.angle2 = angle2 - Math.PI.toFloat()
        }
        RoundedShapeDrawer.drawCircle(x - 5, y - 5, x + diameter + 5, y + diameter + 5, color.rgb)
        *///?}
    }

    /**
     * Method to draw a radial gradient circle.
     *
     * **NOTE:** If you are using [DrawContextUtils.translate] or [DrawContextUtils.scale]
     * with this method, ensure they are invoked in the correct order if you use both. That is, [DrawContextUtils.translate]
     * is called **BEFORE** [DrawContextUtils.scale], otherwise the rectangle will not be rendered correctly
     *
     * @param x The x-coordinate of the circle's top-left bounding box corner.
     * @param y The y-coordinate of the circle's top-left bounding box corner.
     * @param radius The circle's radius.
     * @param startColor The start color of the gradient.
     * @param endColor The end color of the gradient.
     * @param angle defines the angle of the gradient.
     * @param progress the progress of the gradient (0.0 to 1.0)
     * @param phaseOffset the phase offset of the gradient (0.0 to 360.0)
     * @param smoothness smooths out the edge. (In amount of blurred pixels)
     * @param reverse if true, the gradient will be reversed
     */
    fun drawRadialGradientFilledCircle(
        x: Int,
        y: Int,
        radius: Int = 10,
        startColor: ChromaColour,
        endColor: ChromaColour,
        angle: Float = 180f,
        progress: Float,
        phaseOffset: Float,
        smoothness: Float = 1.5f,
        reverse: Boolean = false,
    ) {
        //? if >= 26.1 {
        drawRadialGradientFilledCircleDeferred(x, y, radius, startColor, endColor, angle, progress, phaseOffset, smoothness, reverse)
        //?} else {
        /*val radiusIn = radius * GuiScreenUtils.scaleFactor
        val diameter = radius * 2
        RadialGradientCircleShader.applyBaseSettings(radiusIn, diameter, diameter, x, y, smoothness) {
            this.angle = angle - Math.PI.toFloat()
            this.reverse = if (reverse) 1 else 0
            this.progress = progress
            this.phaseOffset = phaseOffset
            this.startColor = startColor.destructToFloatArray()
            this.endColor = endColor.destructToFloatArray()
        }
        RoundedShapeDrawer.drawGradientCircle(x - 5, y - 5, x + diameter + 5, y + diameter + 5, startColor, endColor)
        *///?}
    }

    private fun buildRoundedStateParams(x: Int, y: Int, width: Int, height: Int, radius: Int): RoundedRenderStateParams {
        val scaleFactor = GuiScreenUtils.scaleFactor
        val halfSizeX = (width * scaleFactor) / 2f
        val halfSizeY = (height * scaleFactor) / 2f
        val centerPosX = (x * scaleFactor) + halfSizeX
        val centerPosY = GuiScreenUtils.displayHeight - ((y * scaleFactor) + halfSizeY)
        val matrix = Matrix3x2f(DrawContextUtils.drawContext.pose())
        val xScale = matrix.m00()
        val yScale = matrix.m11()
        val xTranslation = matrix.m20()
        val yTranslation = matrix.m21()
        return RoundedRenderStateParams(
            radius = radius.toFloat(),
            adjustedHalfSizeX = halfSizeX * xScale,
            adjustedHalfSizeY = halfSizeY * yScale,
            adjustedCenterPosX = (centerPosX * xScale) + (xTranslation * scaleFactor),
            // Y-Scaling affects the center-point of the rounded rect differently than X-Scaling, as it scales from the top edge rather
            // than the center, so we need to adjust the center Y position accordingly before applying translation
            adjustedCenterPosY = (if (yScale != 1f) centerPosY - (halfSizeY * (yScale - 1)) else centerPosY) - (yTranslation * scaleFactor),
            matXScale = xScale,
            matYScale = yScale,
            matXTranslation = xTranslation,
            matYTranslation = yTranslation,
        )
    }

    /**
     * Deferred equivalent of [drawRoundRect]. Captures all shader parameters from the
     * current pose matrix and submits a [SkyHanniRoundedRectRenderState] to the
     * [GuiRenderState] queue, ensuring correct ordering over all other GUI elements.
     */
    fun drawRoundRectDeferred(x: Int, y: Int, width: Int, height: Int, color: Int, radius: Int = 10, smoothness: Float = 1f) {
        if (!tryBlitRoundedRect(x, y, width, height, color, radius, smoothness)) {
            DrawContextUtils.addGuiElement(buildRoundedRectState(x, y, width, height, color, radius, smoothness))
        }
    }

    /**
     * Deferred equivalent of [drawRoundGradientRect]. Captures all shader parameters from the
     * current pose matrix and submits a [SkyHanniRoundedRectRenderState] to the
     * [GuiRenderState] queue.
     *
     * @param topColor the color of the top of the rectangle
     * @param bottomColor the color of the bottom of the rectangle
     * @param radius the radius of the corners (default 10)
     * @param smoothness how smooth the corners will appear (default 1)
     */
    fun drawRoundGradientRectDeferred(
        x: Int, y: Int, width: Int, height: Int,
        topColor: Int,
        bottomColor: Int,
        radius: Int = 10,
        smoothness: Float = 1f,
    ) {
        if (topColor == bottomColor && tryBlitRoundedRect(x, y, width, height, topColor, radius, smoothness)) return
        DrawContextUtils.addGuiElement(
            buildRoundedRectGradientState(x, y, width, height, topColor, bottomColor, radius, smoothness)
        )
    }

    /**
     * Attempts to blit a solid rounded rect from the atlas. Returns true if the shape was already
     * cached and the blit was submitted; returns false on a cache miss, leaving the caller to fall
     * back to a deferred render state submission.
     *
     * @param x left edge in logical GUI pixels
     * @param y top edge in logical GUI pixels
     * @param width width in logical GUI pixels
     * @param height height in logical GUI pixels
     * @param color ARGB packed fill color
     * @param radius corner radius in logical GUI pixels
     * @param smoothness edge smoothness in physical pixels
     */
    private fun tryBlitRoundedRect(
        x: Int, y: Int, width: Int, height: Int,
        color: Int, radius: Int, smoothness: Float,
    ): Boolean {
        val scaleFactor = GuiScreenUtils.scaleFactor
        val matrix = Matrix3x2f(DrawContextUtils.drawContext.pose())
        val pixelWidth = (width * scaleFactor * matrix.m00()).roundToInt()
        val pixelHeight = (height * scaleFactor * matrix.m11()).roundToInt()
        val radiusPixels = (radius * scaleFactor * matrix.m00()).roundToInt()
        val atlasKey = SkyHanniRoundedShapeAtlasKey.RoundedRect(pixelWidth, pixelHeight, color, radiusPixels, smoothness)
        val scissor = DrawContextUtils.drawContext.scissorStack.peek()
        return SkyHanniRoundedShapeRenderManager.submitBlit(
            atlasKey, DrawContextUtils.drawContext.guiRenderState, matrix, x, y, x + width, y + height, -1, scissor,
        )
    }

    /**
     * Deferred equivalent of [drawRoundRectOutline]. Captures all shader parameters from
     * the current pose matrix and submits a [SkyHanniRoundedRectOutlineRenderState] to the
     * [GuiRenderState] queue.
     */
    fun drawRoundRectOutlineDeferred(
        x: Int, y: Int, width: Int, height: Int,
        topColor: Int,
        bottomColor: Int,
        borderThickness: Int,
        radius: Int = 10,
        blur: Float = 0.7f,
    ) {
        val state = buildRoundedRectOutlineState(x, y, width, height, topColor, bottomColor, borderThickness, radius, blur)
        DrawContextUtils.addGuiElement(state)
    }

    /**
     * Deferred equivalent of [drawRoundTexturedRect]. Captures all shader parameters from the
     * current pose matrix and submits a [SkyHanniRoundedTexturedRectRenderState] to the
     * [GuiRenderState] queue.
     *
     * @param radius the radius of the corners (default 10)
     * @param smoothness how smooth the corners will appear (default 1)
     * @param texture the texture identifier to render
     * @param alpha the alpha multiplier (default 1.0)
     */
    fun drawRoundTexturedRectDeferred(
        x: Int, y: Int, width: Int, height: Int,
        radius: Int = 10,
        smoothness: Float = 1f,
        texture: Identifier,
        alpha: Float = 1f,
    ) {
        if (radius <= 0) return GuiRenderUtils.drawTexturedRect(x, y, width, height, texture = texture, alpha = alpha)
        val state = buildRoundedTexturedRectState(x, y, width, height, radius, smoothness, texture, alpha)
        DrawContextUtils.addGuiElement(state)
    }

    /**
     * Deferred equivalent of [drawFilledCircle]. Captures all shader parameters from the
     * current pose matrix and submits a [SkyHanniCircleRenderState] to the [GuiRenderState] queue.
     *
     * @param x The x-coordinate of the circle's top-left bounding box corner.
     * @param y The y-coordinate of the circle's top-left bounding box corner.
     * @param color The fill color.
     * @param radius The circle's radius.
     * @param smoothness smooths out the edge (in amount of blurred pixels).
     * @param angle1 defines the start of the arc, must be in range [0, 2*pi].
     * @param angle2 defines the end of the arc, must be in range [0, 2*pi].
     */
    fun drawFilledCircleDeferred(
        x: Int,
        y: Int,
        color: Color,
        radius: Int = 10,
        smoothness: Float = 1f,
        angle1: Float = 7.0f,
        angle2: Float = 7.0f,
    ) {
        val guiRenderState = DrawContextUtils.drawContext.guiRenderState
        // Only full circles (default angles) are atlased; arcs change per-frame and would overflow the atlas.
        if (angle1 == 7.0f && angle2 == 7.0f) {
            val scaleFactor = GuiScreenUtils.scaleFactor
            val matrix = Matrix3x2f(DrawContextUtils.drawContext.pose())
            val radiusPixels = (radius * scaleFactor * matrix.m00()).roundToInt()
            val atlasKey = SkyHanniRoundedShapeAtlasKey.Circle(radiusPixels, color.rgb, smoothness, angle1, angle2)
            val scissor = DrawContextUtils.drawContext.scissorStack.peek()
            val diameter = radius * 2
            val blitted = SkyHanniRoundedShapeRenderManager.submitBlit(
                atlasKey, guiRenderState, matrix, x, y, x + diameter, y + diameter, -1, scissor,
            )
            if (blitted) return
        }
        //~ if < 26.1 'addGuiElement' -> 'submitGuiElement'
        guiRenderState.addGuiElement(buildCircleState(x, y, radius, color.rgb, smoothness, angle1, angle2))
    }

    /**
     * Deferred equivalent of [drawRadialGradientFilledCircle]. Captures all shader parameters from the
     * current pose matrix and submits a [SkyHanniRadialGradientCircleRenderState] to the [GuiRenderState] queue.
     *
     * @param x The x-coordinate of the circle's top-left bounding box corner.
     * @param y The y-coordinate of the circle's top-left bounding box corner.
     * @param radius The circle's radius.
     * @param startColor The start color of the gradient.
     * @param endColor The end color of the gradient.
     * @param angle defines the start angle of the gradient sweep.
     * @param progress the arc length of the gradient as a fraction of the full circle (0.0 to 1.0).
     * @param phaseOffset the phase offset applied to the color interpolation factor.
     * @param smoothness smooths out the edge (in amount of blurred pixels).
     * @param reverse if true, the gradient color direction will be reversed.
     */
    fun drawRadialGradientFilledCircleDeferred(
        x: Int,
        y: Int,
        radius: Int = 10,
        startColor: ChromaColour,
        endColor: ChromaColour,
        angle: Float = 180f,
        progress: Float,
        phaseOffset: Float,
        smoothness: Float = 1.5f,
        reverse: Boolean = false,
    ) {
        val state = buildGradientCircleState(
            x, y, radius, startColor.destructToFloatArray(), endColor.destructToFloatArray(),
            angle - Math.PI.toFloat(), progress, phaseOffset, smoothness, reverse,
        )
        DrawContextUtils.addGuiElement(state)
    }

    private fun buildRoundedRectState(
        x: Int, y: Int, width: Int, height: Int,
        color: Int, radius: Int, smoothness: Float,
    ): SkyHanniRoundedRectRenderState {
        val params = buildRoundedStateParams(x, y, width, height, radius)
        return SkyHanniRoundedRectRenderState(
            x, y, width, height, color, smoothness, params,
            DrawContextUtils.drawContext.scissorStack.peek(),
        )
    }

    private fun buildRoundedRectGradientState(
        x: Int, y: Int, width: Int, height: Int,
        topColor: Int, bottomColor: Int, radius: Int, smoothness: Float,
    ): SkyHanniRoundedRectRenderState {
        val params = buildRoundedStateParams(x, y, width, height, radius)
        return SkyHanniRoundedRectRenderState(
            x, y, width, height, topColor, smoothness, params,
            DrawContextUtils.drawContext.scissorStack.peek(),
            bottomColor,
        )
    }

    private fun buildRoundedRectOutlineState(
        x: Int, y: Int, width: Int, height: Int,
        topColor: Int, bottomColor: Int, borderThickness: Int, radius: Int, blur: Float,
    ): SkyHanniRoundedRectOutlineRenderState {
        val params = buildRoundedStateParams(x, y, width, height, radius)
        return SkyHanniRoundedRectOutlineRenderState(
            x, y, width, height, topColor, bottomColor,
            borderThickness.toFloat(), max(1 - blur, 0f), params,
            DrawContextUtils.drawContext.scissorStack.peek(),
        )
    }

    private fun buildRoundedTexturedRectState(
        x: Int, y: Int, width: Int, height: Int,
        radius: Int, smoothness: Float, texture: Identifier, alpha: Float,
    ): SkyHanniRoundedTexturedRectRenderState {
        val params = buildRoundedStateParams(x, y, width, height, radius)
        return SkyHanniRoundedTexturedRectRenderState(
            x, y, width, height, params, smoothness, texture, alpha,
            DrawContextUtils.drawContext.scissorStack.peek(),
        )
    }

    private fun buildCircleState(
        x: Int, y: Int, radius: Int,
        color: Int, smoothness: Float, angle1: Float, angle2: Float,
    ): SkyHanniCircleRenderState {
        val diameter = radius * 2
        val params = buildRoundedStateParams(x, y, diameter, diameter, radius)
        return SkyHanniCircleRenderState(
            x, y, diameter, diameter,
            color, smoothness,
            angle1 - Math.PI.toFloat(), angle2 - Math.PI.toFloat(),
            params, DrawContextUtils.drawContext.scissorStack.peek(),
        )
    }

    private fun buildGradientCircleState(
        x: Int, y: Int, radius: Int,
        startColor: FloatArray, endColor: FloatArray,
        angle: Float, progress: Float, phaseOffset: Float, smoothness: Float, reverse: Boolean,
    ): SkyHanniRadialGradientCircleRenderState {
        val diameter = radius * 2
        val params = buildRoundedStateParams(x, y, diameter, diameter, radius)
        return SkyHanniRadialGradientCircleRenderState(
            x, y, diameter, diameter, params,
            smoothness, angle, progress, phaseOffset, reverse,
            startColor, endColor,
            DrawContextUtils.drawContext.scissorStack.peek(),
        )
    }
}
