package at.hannibal2.skyhanni.utils.render.states

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState

/**
 * Wraps any [GuiElementRenderState] and overrides its scissor area,
 * delegating all other behavior to the inner state.
 *
 * @param inner the state whose behavior to delegate to.
 * @param scissor the scissor area to apply, or null for no scissor.
 */
class ScissorDecoratingRenderState(
    private val inner: GuiElementRenderState,
    private val scissor: ScreenRectangle?,
) : GuiElementRenderState {
    override fun bounds(): ScreenRectangle? = inner.bounds()
    override fun scissorArea(): ScreenRectangle? = scissor
    override fun textureSetup(): TextureSetup = inner.textureSetup()
    override fun buildVertices(consumer: VertexConsumer) = inner.buildVertices(consumer)
    override fun pipeline(): RenderPipeline = inner.pipeline()
}
