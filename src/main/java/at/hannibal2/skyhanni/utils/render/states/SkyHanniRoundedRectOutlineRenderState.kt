package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.RoundedShaderParams
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams0
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams1
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState

class SkyHanniRoundedRectOutlineRenderState(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val topColor: Int,
    private val bottomColor: Int,
    private val borderThickness: Float,
    private val borderBlur: Float,
    private val params: RoundedShaderParams,
    private val scissor: ScreenRectangle?,
) : GuiElementRenderState {

    private val borderAdjustment = (borderThickness / 2).toInt()

    override fun bounds(): ScreenRectangle = with(params) {
        ScreenRectangle(
            (matXScale * (x - borderAdjustment) + matXTranslation).toInt(),
            (matYScale * (y - borderAdjustment) + matYTranslation).toInt(),
            ((width + borderAdjustment * 2) * matXScale).toInt(),
            ((height + borderAdjustment * 2) * matYScale).toInt(),
        )
    }

    override fun scissorArea(): ScreenRectangle? = scissor
    override fun pipeline(): RenderPipeline = SkyHanniRenderPipeline.ROUNDED_RECT_OUTLINE_DEFERRED()
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun buildVertices(consumer: VertexConsumer) {
        val left = (x - borderAdjustment).toFloat()
        val top = (y - borderAdjustment).toFloat()
        val right = (x + width + borderAdjustment).toFloat()
        val bottom = (y + height + borderAdjustment).toFloat()

        writeVertex(consumer, left, top, topColor)
        writeVertex(consumer, left, bottom, bottomColor)
        writeVertex(consumer, right, bottom, bottomColor)
        writeVertex(consumer, right, top, topColor)
    }

    private fun writeVertex(consumer: VertexConsumer, vx: Float, vy: Float, color: Int) = with(params) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.setColor(color)
        buf.writeParams0(radius, borderThickness, adjustedHalfSizeX, adjustedHalfSizeY)
        buf.writeParams1(adjustedCenterPosX, adjustedCenterPosY, borderBlur, 0f)
    }
}
