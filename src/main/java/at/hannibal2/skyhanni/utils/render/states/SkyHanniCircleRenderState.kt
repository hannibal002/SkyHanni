package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.VertexElement
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState

/**
 * Deferred render state for a filled circle (or arc). Submits to the GuiRenderState queue,
 * ensuring the circle renders after any screen-level blur effects.
 *
 * @param x The left edge of the circle's bounding box in GUI coordinates.
 * @param y The top edge of the circle's bounding box in GUI coordinates.
 * @param diameter The diameter of the circle in GUI coordinates.
 * @param color The ARGB packed color.
 * @param adjustedRadius The circle radius in physical pixels, pre-scaled by the pose scale.
 * @param smoothness Edge smoothing in physical pixels.
 * @param angle1 Start angle for arc clipping (radians, pre-shifted by -PI).
 * @param angle2 End angle for arc clipping (radians, pre-shifted by -PI).
 * @param adjustedCenterPosX Circle center X in physical pixels (y-up origin), with pose applied.
 * @param adjustedCenterPosY Circle center Y in physical pixels (y-up origin), with pose applied.
 * @param matXScale Horizontal scale from the current GUI pose matrix.
 * @param matYScale Vertical scale from the current GUI pose matrix.
 * @param matXTranslation Horizontal translation from the current GUI pose matrix, in GUI units.
 * @param matYTranslation Vertical translation from the current GUI pose matrix, in GUI units.
 * @param scissor Active scissor rectangle, or null if none.
 */
class SkyHanniCircleRenderState(
    private val x: Int,
    private val y: Int,
    private val diameter: Int,
    private val color: Int,
    private val adjustedRadius: Float,
    private val smoothness: Float,
    private val angle1: Float,
    private val angle2: Float,
    private val adjustedCenterPosX: Float,
    private val adjustedCenterPosY: Float,
    private val matXScale: Float,
    private val matYScale: Float,
    private val matXTranslation: Float,
    private val matYTranslation: Float,
    private val scissor: ScreenRectangle?,
) : GuiElementRenderState {

    private val padding = 5

    override fun pipeline() = SkyHanniRenderPipeline.CIRCLE_DEFERRED()
    override fun scissorArea(): ScreenRectangle? = scissor
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun bounds(): ScreenRectangle = ScreenRectangle(
        (matXScale * (x - padding) + matXTranslation).toInt(),
        (matYScale * (y - padding) + matYTranslation).toInt(),
        ((diameter + padding * 2) * matXScale).toInt(),
        ((diameter + padding * 2) * matYScale).toInt(),
    )

    override fun buildVertices(consumer: VertexConsumer) {
        val p = padding.toFloat()
        writeVertex(consumer, x - p, y - p)
        writeVertex(consumer, x - p, y + diameter + p)
        writeVertex(consumer, x + diameter + p, y + diameter + p)
        writeVertex(consumer, x + diameter + p, y - p)
    }

    private fun writeVertex(consumer: VertexConsumer, vx: Float, vy: Float) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.setColor(color)
        buf.writeParams(adjustedRadius, smoothness, angle1, angle2, VertexElement.ROUNDED_PARAMS_0)
        buf.writeParams(adjustedCenterPosX, adjustedCenterPosY, 0f, 0f, VertexElement.ROUNDED_PARAMS_1)
    }
}
