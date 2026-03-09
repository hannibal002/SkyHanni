package at.hannibal2.skyhanni.utils.renderables.animated.rotate

import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.renderables.SnappedVec3.Companion.toSnapped
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationStorage.Companion.ROTATION_SNAP
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.core.Direction.Axis
import net.minecraft.world.phys.Vec3

/**
 * Stores properties for the rotation definition, and current rotation vector of,
 * an AnimatedItemStackRenderable.
 */
sealed interface AnimatedRotationStorage {
    val rotationDefinition: AnimatedRotationDefinition
    var currentRotation: SnappedVec3

    companion object {
        internal const val ROTATION_SNAP = 0.1
    }
}

open class AnimatedRotationLocalStorage(
    override val rotationDefinition: AnimatedRotationDefinition = AnimatedRotationDefinition(),
) : AnimatedRotationStorage {
    override var currentRotation: SnappedVec3 = Axis.entries.fold(Vec3.ZERO.toSnapped(ROTATION_SNAP)) { vec, axis ->
        val staticRotation = rotationDefinition.getStaticRotation(axis).takeIf { it != 0.0 } ?: return@fold vec
        if (vec[axis] != 0.0) return@fold vec
        vec.applyAxisValue(axis, staticRotation)
    }
    constructor(rotationSpeed: Double) : this(
        AnimatedRotationDefinition(
            Axis.X to AxisRotationDefinition(rotationSpeed),
            Axis.Y to AxisRotationDefinition(rotationSpeed),
            Axis.Z to AxisRotationDefinition(rotationSpeed),
        )
    )
    constructor(vararg definitionPairs: Pair<Axis, AxisRotationDefinition>) : this(AnimatedRotationDefinition(*definitionPairs))
}

open class AnimatedRotationPropertyStorage(
    override val rotationDefinition: AnimatedRotationDefinition = AnimatedRotationDefinition(),
    val propGetter: () -> Property<Vec3>,
) : AnimatedRotationStorage {
    private var staticRotationApplied = false
    override var currentRotation: SnappedVec3
        get() {
            val snapped = propGetter().get().toSnapped(ROTATION_SNAP)
            if (staticRotationApplied) return snapped
            staticRotationApplied = true
            val withStatic = Axis.entries.fold(snapped) { vec, axis ->
                val staticRotation = rotationDefinition.getStaticRotation(axis).takeIf { it != 0.0 } ?: return@fold vec
                if (vec[axis] != 0.0) return@fold vec
                vec.applyAxisValue(axis, staticRotation)
            }
            propGetter().set(withStatic)
            return withStatic
        }
        set(value) = propGetter().set(value)
}

/**
 * A data class that defines the rotation behavior of an item stack.
 *
 * A positive rotation speed will rotate the item counter-clockwise,
 * a negative rotation speed will rotate it clockwise, and a
 * rotation speed of 0.0 will make the item stationary.
 *
 * @param axes A map of axis to rotation definitions, defining the rotation behavior for each axis.
 */
data class AnimatedRotationDefinition(
    private val axes: Map<Axis, AxisRotationDefinition> = mapOf(
        Axis.X to AxisRotationDefinition(),
        Axis.Y to AxisRotationDefinition(),
        Axis.Z to AxisRotationDefinition(),
    )
) : Map<Axis, AxisRotationDefinition> by axes {
    constructor(vararg pairs: Pair<Axis, AxisRotationDefinition>) : this(pairs.asList().associate { it.first to it.second })

    fun isEnabled() = axes.values.any { it.isEnabled() }
    fun isAxisEnabled(axis: Axis) = axes[axis]?.isEnabled() ?: false
    fun getStaticRotation(axis: Axis) = axes[axis]?.staticRotation ?: 0.0
    fun setStaticRotation(axis: Axis, rotation: Double) {
        axes[axis]?.staticRotation = rotation
    }

    fun getRotationOffset(axis: Axis, deltaTime: Double): Double {
        val axisDef = axes[axis] ?: return 0.0
        return axisDef.rotationSpeed * deltaTime
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
    var rotationSpeed: Double = 0.0,
    var staticRotation: Double = 0.0,
) {
    constructor(rotationSpeed: Double) : this(rotationSpeed, 0.0)
    fun isEnabled() = rotationSpeed != 0.0 || staticRotation != 0.0
}
