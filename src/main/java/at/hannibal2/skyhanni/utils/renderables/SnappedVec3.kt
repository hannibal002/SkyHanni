package at.hannibal2.skyhanni.utils.renderables

import net.minecraft.core.Direction.Axis
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

class SnappedVec3(
    private val snappedX: Double = 0.0,
    private val snappedY: Double = 0.0,
    private val snappedZ: Double = 0.0,
    private val snapValue: Double = 1.0,
    private val maxValue: Double = 360.0,
) : Vec3(
    snap(snappedX, snapValue),
    snap(snappedY, snapValue),
    snap(snappedZ, snapValue),
) {
    fun applyAxisValue(axis: Axis, value: Double): SnappedVec3 = when (axis) {
        Axis.X -> copy(snappedX = value % maxValue)
        Axis.Y -> copy(snappedY = value % maxValue)
        Axis.Z -> copy(snappedZ = value % maxValue)
    }

    fun copy(
        snappedX: Double = this.snappedX,
        snappedY: Double = this.snappedY,
        snappedZ: Double = this.snappedZ,
        snapValue: Double = this.snapValue,
        maxValue: Double = this.maxValue,
    ) = SnappedVec3(snappedX, snappedY, snappedZ, snapValue, maxValue)

    fun applyAxisOffset(axis: Axis, offset: Double): SnappedVec3 = applyAxisValue(
        axis,
        offset + when (axis) {
            Axis.X -> snappedX
            Axis.Y -> snappedY
            Axis.Z -> snappedZ
        },
    )

    companion object {
        val ZERO = SnappedVec3(0.0, 0.0, 0.0)

        fun Vec3.toSnapped(snapValue: Double = 1.0, maxValue: Double = 360.0) =
            SnappedVec3(x, y, z, snapValue, maxValue)

        private fun snap(value: Double, snapValue: Double) =
            if (snapValue <= 0.0) value
            else (value / snapValue).roundToInt() * snapValue
    }
}
