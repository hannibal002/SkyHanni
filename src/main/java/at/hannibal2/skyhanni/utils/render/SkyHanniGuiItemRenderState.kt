package at.hannibal2.skyhanni.utils.render

import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3x2f

data class SkyHanniGuiItemRenderState(
    val guiItemRenderState: GuiItemRenderState,
    val x: Float,
    val y: Float,
    val rotationVec: Vec3,
    val translationVec: Vec3,
    val scale: Float,
    val stableId: Int,
) : PictureInPictureRenderState {
    companion object {
        private var counter = 0
        fun nextStableId() = counter++
    }
    private val x0 = x.toInt()
    private val x1 = (x + (scale * 16)).toInt()
    private val y0 = y.toInt()
    private val y1 = (y + (scale * 16)).toInt()

    fun isSkull(): Boolean = this.guiItemRenderState.itemStackRenderState().usesBlockLight()

    fun guiItemRenderState() = guiItemRenderState

    fun guiTrackingState(state: SkyHanniGuiItemRenderState): TrackingItemStackRenderState? =
        state.guiItemRenderState().itemStackRenderState()

    override fun x0() = x0
    override fun x1() = x1
    override fun y0() = y0
    override fun y1() = y1

    override fun scale() = scale * 16

    override fun pose(): Matrix3x2f = guiItemRenderState.pose()

    override fun scissorArea(): ScreenRectangle? = this.guiItemRenderState.scissorArea()
    override fun bounds(): ScreenRectangle? = this.guiItemRenderState.bounds()

}
