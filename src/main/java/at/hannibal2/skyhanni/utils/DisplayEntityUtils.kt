package at.hannibal2.skyhanni.utils

import com.mojang.math.Transformation
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionfc
import org.joml.Vector3f

object DisplayEntityUtils {
    val Display.transformation: Transformation?
        get() {
            return renderState()?.transformation?.get(0f)
        }

    inline val Display.rotation: Vec3 get() = this.lookAngle

    // Do not compare raw Vec3 instances, since it thinks -0.0 != 0.0 which is not what we want here.
    val Display.isRotated: Boolean get() {
        val rotation = this.rotation
        return rotation.x() != 0.0 || rotation.y() != 0.0 || rotation.z() != 1.0
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
}
