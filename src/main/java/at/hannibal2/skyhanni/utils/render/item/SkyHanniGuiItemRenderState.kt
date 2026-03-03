package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAnimatedAtlasKey
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAtlasKey
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.SubmitNodeCollector
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
        appendModelIdentityElement(scale.roundTo(3))
        appendModelIdentityElement(adjustedScale.roundTo(3))
        appendModelIdentityElement(rotationVector)
        // stableId intentionally NOT in modelIdentity. atlas key equality already excludes it,
        // and including it here prevents cache hits for static items across frames.
        frameNumber?.let { appendModelIdentityElement(it) }
        if (rotationVector != Vec3.ZERO || frameNumber != null) setAnimated()
    }
    private val _atlasKey = run {
        val baseKey = SkyHanniAtlasKey(
            item = itemStack.item.toString(),
            modelIdentity = trackingState.modelIdentity,
            scale = scale,
            adjustedScale = adjustedScale,
            rotationVector = rotationVector,
        )
        if (trackingState.isAnimated) SkyHanniAnimatedAtlasKey(baseKey, frameNumber ?: 0)
        else baseKey
    }
    val atlasKey get() = _atlasKey

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

    fun getModelIdentity(): List<*> = trackingState.modelIdentity as List<*>
    fun usesBlockLight(): Boolean = trackingState.usesBlockLight()
    fun isAnimated(): Boolean = trackingState.isAnimated
    fun setAnimated() = trackingState.setAnimated()
    fun submit(matrices: PoseStack, submitNodeCollector: SubmitNodeCollector, i: Int, j: Int, k: Int) =
        this.trackingState.submit(matrices, submitNodeCollector, i, j, k)
}
