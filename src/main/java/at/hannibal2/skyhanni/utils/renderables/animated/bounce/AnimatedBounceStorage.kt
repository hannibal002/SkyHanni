package at.hannibal2.skyhanni.utils.renderables.animated.bounce

import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.renderables.SnappedVec3.Companion.toSnapped
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceStorage.Companion.BOUNCE_SNAP
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.core.Direction.Axis
import net.minecraft.world.phys.Vec3

/**
 * Stores properties, and a getter/setter of a storage location, for the bounce
 * definition, and current bounce offset vector of, an AnimatedItemStackRenderable.
 */
interface AnimatedBounceStorage {
    val bounceDefinition: AnimatedBounceDefinition
    var currentBounce: SnappedVec3

    companion object {
        internal const val BOUNCE_SNAP = 0.05
    }
}

open class AnimatedBounceLocalStorage(
    override var bounceDefinition: AnimatedBounceDefinition = AnimatedBounceDefinition(),
    override var currentBounce: SnappedVec3 = Vec3.ZERO.toSnapped(BOUNCE_SNAP),
) : AnimatedBounceStorage

open class AnimatedBouncePropertyStorage(
    override var bounceDefinition: AnimatedBounceDefinition = AnimatedBounceDefinition(),
    val propGetter: () -> Property<Vec3>,
) : AnimatedBounceStorage {
    override var currentBounce: SnappedVec3
        get() = propGetter().get().toSnapped(BOUNCE_SNAP)
        set(value) = propGetter().set(value)
}

/**
 * A data class that defines the bouncing behavior of an item stack.
 * The render will start in the 'middle' and will move up/down relative to that position.
 *
 * @param axes A map of axis to their bounce definitions. If an axis is not present, it will not be bounced.
 */
data class AnimatedBounceDefinition(
    private val axes: Map<Axis, AxisBounceDefinition> = emptyMap()
) : Map<Axis, AxisBounceDefinition> by axes {
    constructor(vararg pairs: Pair<Axis, AxisBounceDefinition>) : this(pairs.asList().associate { it.first to it.second })

    fun isEnabled() = axes.values.any { it.isEnabled() }
    fun isAxisEnabled(axis: Axis) =
        axes[axis]?.isEnabled() ?: false
    fun getBounceOffset(axis: Axis, sinTheta: Double): Double =
        axes[axis]?.getOffset(sinTheta) ?: 0.0
    fun getBouncePeriod(axis: Axis): Double =
        axes[axis]?.period ?: 1.0
    fun getTotalBounceOffset(axis: Axis): Int =
        totalBounceHeightCache[axis] ?: 0

    private val totalBounceHeightCache: Map<Axis, Int> by lazy {
        Axis.entries.associateWith { axis ->
            axes[axis]?.totalOffset?.toInt() ?: 0
        }
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
    private val bounceOffsetPositive: Double = 0.0,
    private val bounceOffsetNegative: Double = 0.0,
    val speed: Double = 0.0,
) {
    constructor(bounceOffset: Double, speed: Double) : this(bounceOffset, bounceOffset, speed)
    fun isEnabled() = speed > 0.0 && bounceOffsetPositive + bounceOffsetNegative != 0.0

    val totalOffset by lazy { bounceOffsetPositive + bounceOffsetNegative }
    val period by lazy {
        (bounceOffsetNegative + bounceOffsetPositive * 2.0) / speed
    }

    fun getOffset(sinTheta: Double) =
        if (sinTheta >= 0) bounceOffsetPositive
        else bounceOffsetNegative
}
