package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture

class OversizedRotatableItemRenderer(
    bufferSource: MultiBufferSource.BufferSource,
) : PictureInPictureRenderer<OversizedRotatableItemRenderState>(bufferSource) {
    override fun renderToTexture(oversizedRotatableItemRenderState: OversizedRotatableItemRenderState, poseStack: PoseStack) {
        poseStack.scale(1.0F, -1.0F, -1.0F)
        val guiItemRenderState = oversizedRotatableItemRenderState.guiItemRenderState
        val screenRectangle = guiItemRenderState.oversizedItemBounds() ?: return

        val f = (screenRectangle.left() + screenRectangle.right()) / 2.0F
        val g = (screenRectangle.top() + screenRectangle.bottom()) / 2.0F
        val h = guiItemRenderState.x() + 8.0F
        val i = guiItemRenderState.y() + 8.0F
        poseStack.translate((h - f) / 16.0F, (g - i) / 16.0F, 0.0F)
        poseStack.mulPose(oversizedRotatableItemRenderState.rotationVector)

        val gameRenderer = Minecraft.getInstance().gameRenderer ?: return
        val trackingItemStackRenderState = guiItemRenderState.itemStackRenderState()
        gameRenderer.lighting.setupFor(
            if (trackingItemStackRenderState.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT
        )
        val featureRenderDispatcher = gameRenderer.featureRenderDispatcher
        val submitNodeStorage = featureRenderDispatcher.submitNodeStorage
        trackingItemStackRenderState.submit(poseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
    }

    override fun getRenderStateClass() = OversizedRotatableItemRenderState::class.java
    override fun getTextureLabel() = "oversized_rotatable_item"
}
