package at.hannibal2.skyhanni.utils.renderables.animated.bounce

import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.system.LazyVar
import net.minecraft.core.Direction.Axis

/**
 * Stores properties, and a getter/setter of a storage location, for the bounce
 * definition, and current bounce offset vector of, an AnimatedItemStackRenderable.
 */
interface AnimatedBounceStorage {
    val bounceDefinition: AnimatedBounceDefinition
    var currentBounce: SnappedVec3
}

/**
 * A data class that defines the bouncing behavior of an item stack.
 * The render will start in the 'middle' and will move up/down relative to that position.
 *
 * @param upwardBounce The upward bounce distance in pixels.
 * @param downwardBounce The downward bounce distance in pixels.
 * @param bounceSpeed How many pixels the object should move per second.
 */
data class AnimatedBounceDefinition(
    private val axes: Map<Axis, AxisBounceDefinition> = mapOf(
        Axis.X to AxisBounceDefinition(),
        Axis.Y to AxisBounceDefinition(),
        Axis.Z to AxisBounceDefinition(),
    )
) : Map<Axis, AxisBounceDefinition> by axes {
    private var enabled: Boolean by LazyVar { axes.values.any { it.isEnabled() } }
    fun isEnabled() = enabled
    fun isAxisEnabled(axis: Axis) = axes[axis]?.isEnabled() ?: false

    fun getBounceOffset(axis: Axis, sinTheta: Double): Double =
        axes[axis]?.getOffset(sinTheta) ?: 0.0
    fun getBouncePeriod(axis: Axis): Double = axes[axis]?.period ?: 1.0

    private val totalBounceHeightCache = mutableMapOf<Axis, Int>()
    fun getTotalBounceHeight(axis: Axis): Int = totalBounceHeightCache.getOrPut(axis) {
        axes[axis]?.let {
            it.bounceOffsetPositive + it.bounceOffsetNegative
        }?.toInt() ?: 0
    }
}

/**
 * A data class that defines the bouncing behavior of one axis an item stack.
 * The render will start in the 'middle' and will move up/down the axis, relative to that position.
 *
 * @param bounceOffsetPositive How many pixels the bounce will "up" from the middle position.
 * @param bounceOffsetNegative How many pixels the bounce will "down" from the middle position
 * @param speed How many pixels the item should move per second.
 */
data class AxisBounceDefinition(
    val bounceOffsetPositive: Double = 0.0,
    val bounceOffsetNegative: Double = 0.0,
    val speed: Double = 0.0,
) {
    constructor(bounceOffset: Double, speed: Double) : this(bounceOffset, bounceOffset, speed)
    private var enabled by LazyVar { speed > 0.0 && bounceOffsetPositive + bounceOffsetNegative != 0.0 }
    fun isEnabled() = enabled

    val period by lazy {
        (bounceOffsetNegative + bounceOffsetPositive * 2.0) / speed
    }

    fun getOffset(sinTheta: Double) =
        if (sinTheta >= 0) bounceOffsetPositive
        else bounceOffsetNegative
}
