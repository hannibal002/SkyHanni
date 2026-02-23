package at.hannibal2.skyhanni.utils.renderables

import net.minecraft.core.Direction.Axis
import net.minecraft.world.phys.Vec3

data class SnappedVec3(
    private var snappedX: Double = 0.0,
    private var snappedY: Double = 0.0,
    private var snappedZ: Double = 0.0,
    val snapValue: Double = 1.0,
    val maxValue: Double = 360.0,
) : Vec3(snap(snappedX, snapValue), snap(snappedY, snapValue), snap(snappedZ, snapValue)) {
    fun applyAxisValue(axis: Axis, value: Double): SnappedVec3 = when (axis) {
        Axis.X -> copy(snappedX = value % maxValue)
        Axis.Y -> copy(snappedY = value % maxValue)
        Axis.Z -> copy(snappedZ = value % maxValue)
    }

    private fun snap(value: Double) = snap(value, snapValue)
    companion object {
        val ZERO = SnappedVec3(0.0, 0.0, 0.0)

        fun Vec3.toSnapped(snapValue: Double = 1.0, maxValue: Double = 360.0) =
            SnappedVec3(x, y, z, snapValue, maxValue)

        private fun snap(value: Double, snapValue: Double) =
            if (snapValue <= 0.0) value
            else Math.round(value / snapValue) * snapValue
    }
}
