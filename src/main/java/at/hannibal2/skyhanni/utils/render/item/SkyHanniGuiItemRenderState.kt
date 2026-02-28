package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAtlasKey
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3x2f

data class SkyHanniGuiItemRenderState(
    val guiItemRenderState: GuiItemRenderState,
    val x: Float,
    val y: Float,
    val rotationVec: Vec3,
    private val translationVec: Vec3,
    val scale: Float = 1f,
    // Adjusted scale must account for the GUI Scale from SH editor
    val adjustedScale: Float = 1f,
    private val passedStableId: Int? = null,
) : PictureInPictureRenderState {
    companion object {
        private var counter = 0
        fun nextStableId() = counter++
    }

    private val trackingState: TrackingItemStackRenderState? by lazy { guiItemRenderState.itemStackRenderState() }
    val stableId = passedStableId?.takeIf { it >= 0 } ?: nextStableId()

    fun getAtlasKey(guiScale: Int): SkyHanniAtlasKey? = trackingState?.let {
        SkyHanniAtlasKey(it.modelIdentity, scale, guiScale, stableId, rotationVec)
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

    override fun scissorArea(): ScreenRectangle? = this.guiItemRenderState.scissorArea()
    override fun bounds(): ScreenRectangle? = this.guiItemRenderState.bounds()?.let { cb ->
        ScreenRectangle(
            (cb.position.x + translationVec.x).toInt(),
            (cb.position.y + translationVec.y).toInt(),
            (cb.width * adjustedScale).toInt(),
            (cb.height * adjustedScale).toInt(),
        )
    }

    fun getModelIdentity(): Any? = this.trackingState?.modelIdentity
    fun usesBlockLight(): Boolean = this.trackingState?.usesBlockLight() ?: false
    fun isAnimated(): Boolean = this.trackingState?.isAnimated ?: false
    fun setAnimated() = this.trackingState?.setAnimated()
    fun submit(matrices: PoseStack, submitNodeCollector: SubmitNodeCollector, i: Int, j: Int, k: Int) =
        this.trackingState?.submit(matrices, submitNodeCollector, i, j, k)
}
