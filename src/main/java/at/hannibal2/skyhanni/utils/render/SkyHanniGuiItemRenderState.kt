package at.hannibal2.skyhanni.utils.render

import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import org.joml.Matrix3x2f

data class SkyHanniGuiItemRenderState(
    val guiItemRenderState: GuiItemRenderState,
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int,
    val rotX: Float,
    val rotY: Float,
    val rotZ: Float,
    val scale: Float
) : PictureInPictureRenderState {

    fun guiItemRenderState() = guiItemRenderState

    override fun x0() = x0
    override fun x1() = x1

    override fun y0() = y0
    override fun y1() = y1

    override fun scale() = scale * 16

    override fun pose(): Matrix3x2f = guiItemRenderState.pose()

    override fun scissorArea(): ScreenRectangle? {
        return this.guiItemRenderState.scissorArea()
    }

    override fun bounds(): ScreenRectangle? {
        return this.guiItemRenderState.bounds()
    }

}
