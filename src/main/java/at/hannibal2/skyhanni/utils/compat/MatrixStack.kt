package at.hannibal2.skyhanni.utils.compat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.Vec3
import java.nio.FloatBuffer

// todo needs 1.21 impl
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

    //#if MC < 1.21
    fun rotate(angle: Float, x: Double, y: Double, z: Double) {
        GlStateManager.rotate(angle, x.toFloat(), y.toFloat(), z.toFloat())
    }

    fun rotate(angle: Float, x: Float, y: Float, z: Float) {
        GlStateManager.rotate(angle, x, y, z)
    }

    fun rotate(angle: Float, vec: Vec3) {
        this.rotate(angle, vec.xCoord, vec.yCoord, vec.zCoord)
    }

    fun multMatrix(matrix: FloatBuffer) = GlStateManager.multMatrix(matrix)

    fun getFloat(pName: Int, params: FloatBuffer) {
        GlStateManager.getFloat(pName, params)
    }
    //#endif

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
