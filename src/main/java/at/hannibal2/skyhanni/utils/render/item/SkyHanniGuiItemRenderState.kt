package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.render.PoseStackUtils.mulPose
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAnimatedAtlasKey
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAtlasKey
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3x2f

data class SkyHanniGuiItemRenderState(
    val itemStack: ItemStack,
    val guiItemRenderState: GuiItemRenderState,
    val x: Float,
    val y: Float,
    val rotationVector: Vec3,
    private val translationVec: Vec3,
    val scale: Float = 1f,
    // Adjusted scale must account for the GUI Scale from SH editor
    val adjustedScale: Float = 1f,
    private val passedStableId: Int? = null,
    private val frameNumber: Int? = null,
    val alpha: Float = 1f,
) : PictureInPictureRenderState {
    companion object {
        private var counter = 0
        fun nextStableId() = counter++
    }

    val stableId: Int = passedStableId?.takeIf { it >= 0 } ?: nextStableId()
    private val trackingState = guiItemRenderState.itemStackRenderState().apply {
        val itemStack = this@SkyHanniGuiItemRenderState.itemStack
        itemStack.getSkullTexture()?.let { appendModelIdentityElement(it) }
        itemStack.getSkullOwner()?.let { appendModelIdentityElement(it) }
        appendModelIdentityElement(rotationVector)
        // stableId intentionally NOT in modelIdentity. atlas key equality already excludes it,
        // and including it here prevents cache hits for static items across frames.
        frameNumber?.let { appendModelIdentityElement(it) }
        if (rotationVector != Vec3.ZERO || frameNumber != null) setAnimated()
    }
    val atlasKey by lazy {
        val baseKey = SkyHanniAtlasKey(
            item = itemStack.item.toString(),
            modelIdentity = trackingState.modelIdentity,
            rotationVector = rotationVector,
        )
        if (trackingState.isAnimated) SkyHanniAnimatedAtlasKey(baseKey, frameNumber ?: 0)
        else baseKey
    }

    private val x0 = x.toInt()
    private val x1 = (x + (scale * 16)).toInt()
    private val y0 = y.toInt()
    private val y1 = (y + (scale * 16)).toInt()

    override fun x0() = x0
    override fun x1() = x1
    override fun y0() = y0
    override fun y1() = y1

    override fun scale() = scale * 16
    override fun pose(): Matrix3x2f {
        val base = guiItemRenderState.pose()
        if (translationVec.y == 0.0 && translationVec.x == 0.0) return base
        return Matrix3x2f(base).translate(translationVec.x.toFloat(), translationVec.y.toFloat())
    }

    override fun scissorArea(): ScreenRectangle? = guiItemRenderState.scissorArea()
    override fun bounds(): ScreenRectangle? = guiItemRenderState.bounds()?.let { cb ->
        ScreenRectangle(
            (cb.position.x + translationVec.x).toInt(),
            (cb.position.y + translationVec.y).toInt(),
            (cb.width * adjustedScale).toInt(),
            (cb.height * adjustedScale).toInt(),
        )
    }

    fun isAnimated(): Boolean = trackingState.isAnimated
    private fun setAnimated() = trackingState.setAnimated()

    internal fun renderItemToTexture(
        bufferSource: MultiBufferSource.BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        centerX: Float,
        centerY: Float,
        pixelSize: Int,
    ) {
        val ps = PoseStack()
        ps.translate(centerX, centerY, 0.0f)

        val f = pixelSize.toFloat()
        ps.scale(f, -f, f)

        val rotationPadding = if (rotationVector != Vec3.ZERO) 1.0f / 1.42f else 1.0f
        ps.scale(rotationPadding, rotationPadding, rotationPadding)

        val rotated = ps.mulPose(rotationVector)
        ps.translate(0.0f, 0.03f, 0.125f)

        Minecraft.getInstance().gameRenderer.lighting.setupFor(
            if (trackingState.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT,
        )
        if (rotated) setAnimated()

        trackingState.submit(ps, featureRenderDispatcher.submitNodeStorage, 15728880, OverlayTexture.NO_OVERLAY, 0)
        featureRenderDispatcher.renderAllFeatures()
        bufferSource.endBatch()
    }
}
