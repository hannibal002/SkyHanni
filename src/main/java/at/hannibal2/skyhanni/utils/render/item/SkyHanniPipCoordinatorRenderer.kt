package at.hannibal2.skyhanni.utils.render.item

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.MultiBufferSource

class SkyHanniPipCoordinatorRenderer(
    bufferSource: MultiBufferSource.BufferSource,
) : PictureInPictureRenderer<SkyHanniGuiItemRenderState>(bufferSource) {
    companion object {
        private val pendingStates = ArrayList<SkyHanniGuiItemRenderState>(256)
    }

    fun takePendingStates(): List<SkyHanniGuiItemRenderState> = synchronized(pendingStates) {
        val result = ArrayList(pendingStates)
        pendingStates.clear()
        return result
    }

    override fun textureIsReadyToBlit(state: SkyHanniGuiItemRenderState): Boolean = synchronized(pendingStates) {
        pendingStates.add(state)
        return true
    }

    override fun renderToTexture(state: SkyHanniGuiItemRenderState, poseStack: PoseStack) = Unit
    override fun blitTexture(state: SkyHanniGuiItemRenderState, guiRenderState: GuiRenderState) = Unit
    override fun getRenderStateClass(): Class<SkyHanniGuiItemRenderState> = SkyHanniGuiItemRenderState::class.java
    override fun getTranslateY(i: Int, j: Int): Float = i / 2.0f
    override fun getTextureLabel(): String = "skyhanni_item_coordinator"
}
