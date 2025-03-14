package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.Vec3

class DrawContext {
    private val matrices = MatrixStack()
    fun getMatrices(): MatrixStack {
        return this.matrices
    }
}

/**
 * This class has to match up with the vanilla MatrixStack class in modern
 * This means you cant put in sort of helper methods
 */
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

    fun push() {
        GlStateManager.pushMatrix()
    }

    fun pop() {
        GlStateManager.popMatrix()
    }

    // dont think we can do these in 1.8?
    // public void multiply(Quaternionf quaternion)
    // public Entry peek()
    // public boolean isEmpty()
    // public void loadIdentity()
    // public void multiplyPositionMatrix(Matrix4f matrix)

}
