package at.hannibal2.skyhanni.utils

import com.mojang.math.Transformation
import net.minecraft.world.entity.Display
import org.joml.Quaternionfc
import org.joml.Vector3f

object DisplayEntityUtils {
    val Display.transformation: Transformation?
        get() {
            return renderState()?.transformation?.get(0f)
        }

    fun Display.TextDisplay.arrowForwardVec(): LorenzVec {
        val quat = transformation?.leftRotation() ?: return LorenzVec(0, 0, 1)
        val localY = Vector3f(0f, 1f, 0f)
        quat.transform(localY)
        return LorenzVec(localY.x.toDouble(), 0.0, localY.z.toDouble()).normalize()
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

    val Quaternionfc.isIdentity: Boolean
        get() = this.w() == 1f && this.x() == 0f && this.y() == 0f && this.z() == 0f
}
