package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.ChatUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

object PoseStackUtils {

    fun PoseStack.pushPopPose(block: PoseStack.() -> Unit) {
        pushPose()
        block()
        popPose()
    }

    fun PoseStack.mulPose(rotationVector: Vec3) {
        val quaternionf = Quaternionf()
        val xRad = Math.toRadians((rotationVector.x % 360)).toFloat()
        val yRad = Math.toRadians((rotationVector.y % 360)).toFloat()
        val zRad = Math.toRadians((rotationVector.z % 360)).toFloat()
        quaternionf.rotateXYZ(xRad, yRad, zRad)
        mulPose(quaternionf)
    }

    fun PoseStack.defaultAngleDown() = mulPose(Vec3(30.0, 45.0, 0.0))
}
