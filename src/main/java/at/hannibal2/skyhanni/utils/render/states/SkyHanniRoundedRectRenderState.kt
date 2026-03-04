package at.hannibal2.skyhanni.utils.render.states

import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams0
import at.hannibal2.skyhanni.utils.render.SkyHanniVertexFormats.writeParams1
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState

class SkyHanniRoundedRectRenderState(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val color: Int,
    private val smoothness: Float,
    private val params: RoundedRenderStateParams,
    private val scissor: ScreenRectangle?,
) : GuiElementRenderState {

    override fun bounds(): ScreenRectangle = with(params) {
        ScreenRectangle(
            (matXScale * (x - 5) + matXTranslation).toInt(),
            (matYScale * (y - 5) + matYTranslation).toInt(),
            ((width + 10) * matXScale).toInt(),
            ((height + 10) * matYScale).toInt(),
        )
    }

    override fun scissorArea(): ScreenRectangle? = scissor
    override fun pipeline(): RenderPipeline = SkyHanniRenderPipeline.ROUNDED_RECT_DEFERRED()
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun buildVertices(consumer: VertexConsumer) {
        writeVertex(consumer, (x - 5).toFloat(), (y - 5).toFloat())
        writeVertex(consumer, (x - 5).toFloat(), (y + height + 5).toFloat())
        writeVertex(consumer, (x + width + 5).toFloat(), (y + height + 5).toFloat())
        writeVertex(consumer, (x + width + 5).toFloat(), (y - 5).toFloat())
    }

    private fun writeVertex(consumer: VertexConsumer, vx: Float, vy: Float) = with(params) {
        val buf = consumer as BufferBuilder
        buf.addVertex(matXScale * vx + matXTranslation, matYScale * vy + matYTranslation, 0f)
        buf.setColor(color)
        buf.writeParams0(radius, smoothness, adjustedHalfSizeX, adjustedHalfSizeY)
        buf.writeParams1(adjustedCenterPosX, adjustedCenterPosY, 0f, 0f)
    }
}
