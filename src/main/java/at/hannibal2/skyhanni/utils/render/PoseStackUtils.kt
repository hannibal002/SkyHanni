package at.hannibal2.skyhanni.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

object PoseStackUtils {

    /**
     * Rotates the PoseStack by the given rotation vector (in degrees).
     * Returns true if a rotation was applied, false if the rotation vector was (0,0,0).
     */
    fun PoseStack.mulPose(rotationVector: Vec3): Boolean {
        val quaternionf = Quaternionf()
        val xRad = Math.toRadians((rotationVector.x % 360)).toFloat()
        val yRad = Math.toRadians((rotationVector.y % 360)).toFloat()
        val zRad = Math.toRadians((rotationVector.z % 360)).toFloat()

        if (xRad == 0f && yRad == 0f && zRad == 0f) return false

        quaternionf.rotateXYZ(xRad, yRad, zRad)
        mulPose(quaternionf)
        return true
    }
}
