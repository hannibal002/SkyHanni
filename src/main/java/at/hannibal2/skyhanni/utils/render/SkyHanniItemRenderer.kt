package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.render.PoseStackUtils.angleSkullDown
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

    @Suppress("MemberVisibilityCanBePrivate")
    fun renderToTexture(itemRenderState: SkyHanniGuiItemRenderState) {
        // We ignore the passed poseStack, since its transformations make it borderline impossible to perform
        // precise rotations in 3D space, due to unpredictable matrix offsets on the passed stack.
        val identityPoseStack = PoseStack()

        // Copy PIP rendering pattern

        // Translation
        val i = Minecraft.getInstance().window.guiScale
        val j = (itemRenderState.x1() - itemRenderState.x0()) * i
        val k = (itemRenderState.y1() - itemRenderState.y0()) * i
        identityPoseStack.translate(j / 2.0f, k / 2.0f, 0.0f)

        // Scale
        val f = i * itemRenderState.scale()
        identityPoseStack.scale(f, f, -f)
        identityPoseStack.scale(1.0f, -1.0f, -1.0f)

        // Default rotation for skulls
        if (itemRenderState.isSkull()) identityPoseStack.angleSkullDown()

        // Rotation
        val rotated = identityPoseStack.mulPose(itemRenderState.rotationVec)
        identityPoseStack.translate(0.0f, 0.03f, 0.125f)

        val gameRenderer = Minecraft.getInstance().gameRenderer
        val trackingItemStackRenderState = itemRenderState.guiItemRenderState().itemStackRenderState()
        gameRenderer.lighting.setupFor(
            if (trackingItemStackRenderState.usesBlockLight()) Lighting.Entry.ITEMS_3D
            else Lighting.Entry.ITEMS_FLAT
        )
        if (rotated) trackingItemStackRenderState.setAnimated()

        val featureRenderDispatcher = gameRenderer.featureRenderDispatcher
        val submitNodeStorage = featureRenderDispatcher.submitNodeStorage
        trackingItemStackRenderState.submit(identityPoseStack, submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        this.modelOnTextureIdentity = trackingItemStackRenderState.modelIdentity
    }

    override fun renderToTexture(itemRenderState: SkyHanniGuiItemRenderState, poseStack: PoseStack) =
        renderToTexture(itemRenderState)

    override fun blitTexture(itemRenderState: SkyHanniGuiItemRenderState, guiRenderState: GuiRenderState) {
        super.blitTexture(itemRenderState, guiRenderState)
        this.usedOnThisFrame = true
    }

    override fun textureIsReadyToBlit(itemRenderState: SkyHanniGuiItemRenderState): Boolean {
        // Todo, real logic here instead of just always returning false,
        // CBF to setup real state tracking for now
        return false
        //val trackingItemStackRenderState = itemRenderState.guiItemRenderState().itemStackRenderState()
        //return !trackingItemStackRenderState.isAnimated && trackingItemStackRenderState.modelIdentity == this.modelOnTextureIdentity
    }

    override fun getTranslateY(i: Int, j: Int): Float {
        return i / 2.0f
    }

    override fun getTextureLabel(): String {
        return "skyhanni_item_renderer"
    }
}
