package at.hannibal2.skyhanni.utils.render.states

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState

abstract class AbstractSkyHanniRoundedRectRenderState(
    protected val x: Int,
    protected val y: Int,
    protected val width: Int,
    protected val height: Int,
    protected val params: RoundedRenderStateParams,
    private val scissor: ScreenRectangle?,
) : GuiElementRenderState {

    protected abstract val padding: Int

    override fun bounds(): ScreenRectangle = with(params) {
        ScreenRectangle(
            (matXScale * (x - padding) + matXTranslation).toInt(),
            (matYScale * (y - padding) + matYTranslation).toInt(),
            ((width + padding * 2) * matXScale).toInt(),
            ((height + padding * 2) * matYScale).toInt(),
        )
    }

    override fun scissorArea(): ScreenRectangle? = scissor
    override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

    override fun buildVertices(consumer: VertexConsumer) {
        val p = padding.toFloat()
        writeVertex(consumer, (x - p), (y - p), isTop = true)
        writeVertex(consumer, (x - p), (y + height + p), isTop = false)
        writeVertex(consumer, (x + width + p), (y + height + p), isTop = false)
        writeVertex(consumer, (x + width + p), (y - p), isTop = true)
    }

    protected abstract fun writeVertex(consumer: VertexConsumer, vx: Float, vy: Float, isTop: Boolean)
}
