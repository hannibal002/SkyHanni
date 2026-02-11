package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture

class SkyHanniItemRenderer(bufferSource: MultiBufferSource.BufferSource) : PictureInPictureRenderer<SkyHanniGuiItemRenderState>(bufferSource) {

    private var usedOnThisFrame = false
    private var modelOnTextureIdentity: Any? = null

    fun usedOnThisFrame() = this.usedOnThisFrame

    fun resetUsedOnThisFrame() {
        this.usedOnThisFrame = false
    }

    fun invalidateTexture() {
        this.modelOnTextureIdentity = null
    }

    override fun getRenderStateClass() = SkyHanniGuiItemRenderState::class.java

    override fun renderToTexture(itemRenderState: SkyHanniGuiItemRenderState, poseStack: PoseStack) {
        poseStack.scale(1.0f, -1.0f, -1.0f)
        poseStack.mulPose(itemRenderState.rotVec)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        val trackingItemStackRenderState = itemRenderState.guiItemRenderState().itemStackRenderState()
        gameRenderer.lighting.setupFor(
            if (trackingItemStackRenderState.usesBlockLight()) Lighting.Entry.ITEMS_3D
            else Lighting.Entry.ITEMS_FLAT
        )

        val featureRenderDispatcher = gameRenderer.featureRenderDispatcher
        val submitNodeStorage = featureRenderDispatcher.submitNodeStorage
        trackingItemStackRenderState.submit(poseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        this.modelOnTextureIdentity = trackingItemStackRenderState.modelIdentity
    }

    override fun blitTexture(itemRenderState: SkyHanniGuiItemRenderState, guiRenderState: GuiRenderState) {
        super.blitTexture(itemRenderState, guiRenderState)
        this.usedOnThisFrame = true
    }

    override fun textureIsReadyToBlit(itemRenderState: SkyHanniGuiItemRenderState): Boolean {
        val trackingItemStackRenderState = itemRenderState.guiItemRenderState().itemStackRenderState()
        return !trackingItemStackRenderState.isAnimated && trackingItemStackRenderState.modelIdentity == this.modelOnTextureIdentity
    }

    override fun getTranslateY(i: Int, j: Int): Float {
        return i / 2.0f
    }

    override fun getTextureLabel(): String {
        return "skyhanni_item_renderer"
    }
}
