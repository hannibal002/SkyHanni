package at.hannibal2.skyhanni.utils.render.item

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.state.gui.GuiRenderState

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeCollector
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource
*///?}

@Suppress("EmptyDefaultConstructor")
class SkyHanniPipCoordinatorRenderer(
    //? if < 26.2
    //bufferSource: MultiBufferSource.BufferSource,
) : PictureInPictureRenderer<SkyHanniGuiItemRenderState>(
    //? if < 26.2
    //bufferSource,
) {

    companion object {
        private val pendingStates = ArrayList<SkyHanniGuiItemRenderState>(256)
    }

    override fun textureIsReadyToBlit(state: SkyHanniGuiItemRenderState): Boolean {
        pendingStates.add(state)
        return true
    }

    fun peekPendingStates(): List<SkyHanniGuiItemRenderState> = pendingStates.toList()

    fun clearPendingStates() = pendingStates.clear()

    override fun renderToTexture(
        state: SkyHanniGuiItemRenderState,
        poseStack: PoseStack,
        //? if >= 26.2
        submitNodeCollector: SubmitNodeCollector,
    ) = Unit
    override fun blitTexture(state: SkyHanniGuiItemRenderState, guiRenderState: GuiRenderState) = Unit
    override fun getRenderStateClass(): Class<SkyHanniGuiItemRenderState> = SkyHanniGuiItemRenderState::class.java
    override fun getTranslateY(i: Int, j: Int): Float = i / 2.0f
    override fun getTextureLabel(): String = "skyhanni_item_coordinator"
}
