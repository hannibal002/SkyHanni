package at.hannibal2.skyhanni.utils.compat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.Vec3

class MatrixStack {

    fun translate(x: Double, y: Double, z: Double) {
        GlStateManager.translate(x, y, z)
    }

    fun translate(x: Float, y: Float, z: Float) {
        GlStateManager.translate(x, y, z)
    }

    fun translate(vec: Vec3) {
        this.translate(vec.xCoord, vec.yCoord, vec.zCoord)
    }

    fun scale(x: Float, y: Float, z: Float) {
        GlStateManager.scale(x, y, z)
    }

    fun pushMatrix() {
        GlStateManager.pushMatrix()
    }

    fun popMatrix() {
        GlStateManager.popMatrix()
    }

    fun loadIdentity() = GlStateManager.loadIdentity()
}
