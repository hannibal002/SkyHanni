package at.hannibal2.skyhanni.utils

import com.mojang.math.Transformation
import net.minecraft.world.entity.Display
import org.joml.Quaternionfc
import org.joml.Vector3f

object DisplayEntityUtils {
    val Display.transformation: Transformation
        get() {
            // Stuff like "CheckRenderEntityEvent" is called before the entity is ticked,
            // so the renderState may be null. In that case, we create a fresh renderState and use that.
            val currentRenderState = this.renderState ?: run {
                createFreshRenderState().also { this.renderState = it }
            }

            return currentRenderState.transformation.get(0f)
        }

    val Transformation.uniformScale: Float?
        get() {
            val scaleTransform = this.scale()
            val scale = scaleTransform.x()
            return if (scale == scaleTransform.y() && scale == scaleTransform.z()) scale else null
        }

    val Transformation.isRotated: Boolean
        get() {
            return !leftRotation().isIdentity || !rightRotation().isIdentity
        }

    fun Display.TextDisplay.arrowForwardVec(): LorenzVec {
        val quat = transformation.leftRotation()
        val localY = Vector3f(0f, 1f, 0f)
        quat.transform(localY)
        return LorenzVec(localY.x.toDouble(), 0.0, localY.z.toDouble()).normalize()
    }

    val Quaternionfc.isIdentity: Boolean
        get() = this.w() == 1f && this.x() == 0f && this.y() == 0f && this.z() == 0f
}
