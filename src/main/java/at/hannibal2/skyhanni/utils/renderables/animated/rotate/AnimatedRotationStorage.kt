package at.hannibal2.skyhanni.utils.renderables.animated.rotate

import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.system.LazyVar
import net.minecraft.core.Direction.Axis

/**
 * Stores properties, and a getter/setter of a storage location, for the rotation
 * definition, and current rotation vector of, an AnimatedItemStackRenderable.
 */
interface AnimatedRotationStorage {
    val rotationDefinition: AnimatedRotationDefinition
    var currentRotation: SnappedVec3
}

/**
 * A data class that defines the rotation behavior of an item stack.
 *
 * A positive rotation speed will rotate the item counter-clockwise,
 * a negative rotation speed will rotate it clockwise, and a
 * rotation speed of 0.0 will make the item stationary.
 *
 * @param axis The axis around which the item stack will rotate.
 * @param rotationSpeed How many degrees the item should rotate per second.
 */
data class AnimatedRotationDefinition(
    private val axes: Map<Axis, AxisRotationDefinition> = mapOf(
        Axis.X to AxisRotationDefinition(),
        Axis.Y to AxisRotationDefinition(),
        Axis.Z to AxisRotationDefinition(),
    )
) : Map<Axis, AxisRotationDefinition> by axes {
    private var enabled: Boolean by LazyVar { axes.values.any { it.isEnabled() } }
    fun isEnabled() = enabled
    fun isAxisEnabled(axis: Axis) = axes[axis]?.isEnabled() ?: false

    fun getRotation(axis: Axis, deltaTime: Double): Double {
        val axisDef = axes[axis] ?: return 0.0
        return axisDef.staticRotation + (axisDef.rotationSpeed * deltaTime)
    }
}

/**
 * A data class that defines the rotation behavior of an item stack.
 * A positive rotation speed will rotate the item counter-clockwise.
 *
 * @param staticRotation A static rotation offset to apply to the item stack, in degrees.
 * @param rotationSpeed How many degrees the item should rotate per second.
 */
data class AxisRotationDefinition(
    val staticRotation: Double = 0.0,
    val rotationSpeed: Double = 0.0,
) {
    fun isEnabled() = rotationSpeed != 0.0 || staticRotation != 0.0
}
