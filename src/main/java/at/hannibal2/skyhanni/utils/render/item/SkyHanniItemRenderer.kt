package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture

class SkyHanniItemRenderer(
    bufferSource: MultiBufferSource.BufferSource
) : PictureInPictureRenderer<SkyHanniGuiItemRenderState>(bufferSource) {

    private var usedOnThisFrame = false
    private var modelOnTextureIdentity: Any? = null

    override fun getRenderStateClass() = SkyHanniGuiItemRenderState::class.java

    @Suppress("MemberVisibilityCanBePrivate")
    fun renderToTexture(shItemRenderState: SkyHanniGuiItemRenderState) {
        // We ignore the passed poseStack, since its transformations make it borderline impossible to perform
        // precise rotations in 3D space, due to unpredictable matrix offsets on the passed stack.
        val identityPoseStack = PoseStack()

        // Copy PIP rendering pattern

        // Translation
        val i = Minecraft.getInstance().window.guiScale
        val j = (shItemRenderState.x1() - shItemRenderState.x0()) * i
        val k = (shItemRenderState.y1() - shItemRenderState.y0()) * i
        identityPoseStack.translate(j / 2.0f, k / 2.0f, 0.0f)

        // Scale
        val f = i * shItemRenderState.scale()
        identityPoseStack.scale(f, f, -f)
        identityPoseStack.scale(1.0f, -1.0f, -1.0f)

        // Rotation
        val rotated = identityPoseStack.mulPose(shItemRenderState.rotationVec)
        identityPoseStack.translate(0.0f, 0.03f, 0.125f)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        gameRenderer.lighting.setupFor(
            if (shItemRenderState.usesBlockLight()) Lighting.Entry.ITEMS_3D
            else Lighting.Entry.ITEMS_FLAT
        )
        if (rotated) shItemRenderState.setAnimated()

        val featureRenderDispatcher = gameRenderer.featureRenderDispatcher
        val submitNodeStorage = featureRenderDispatcher.submitNodeStorage
        shItemRenderState.submit(identityPoseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        this.modelOnTextureIdentity = shItemRenderState.getModelIdentity()
    }

    override fun renderToTexture(itemRenderState: SkyHanniGuiItemRenderState, poseStack: PoseStack) =
        renderToTexture(itemRenderState)

    override fun blitTexture(itemRenderState: SkyHanniGuiItemRenderState, guiRenderState: GuiRenderState) {
        super.blitTexture(itemRenderState, guiRenderState)
        this.usedOnThisFrame = true
    }

    override fun textureIsReadyToBlit(itemRenderState: SkyHanniGuiItemRenderState): Boolean {
        return !itemRenderState.isAnimated() && itemRenderState.getModelIdentity() == this.modelOnTextureIdentity
    }

    override fun getTranslateY(i: Int, j: Int): Float {
        return i / 2.0f
    }

    override fun getTextureLabel(): String {
        return "skyhanni_item_renderer"
    }
}
