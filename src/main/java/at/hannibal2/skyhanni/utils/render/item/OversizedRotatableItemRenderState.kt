package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.world.phys.Vec3

@JvmRecord
data class OversizedRotatableItemRenderState(
    val guiItemRenderState: GuiItemRenderState,
    val rotationVector: Vec3,
) : PictureInPictureRenderState {
    override fun bounds(): ScreenRectangle = this.guiItemRenderState.bounds() ?: ScreenRectangle(0, 0, 16, 16)

    override fun x0(): Int = bounds().left()
    override fun x1(): Int = bounds().right()
    override fun y0(): Int = bounds().top()
    override fun y1(): Int = bounds().bottom()

    override fun scale(): Float = 16.0f
    override fun scissorArea(): ScreenRectangle? = this.guiItemRenderState.scissorArea()
}
