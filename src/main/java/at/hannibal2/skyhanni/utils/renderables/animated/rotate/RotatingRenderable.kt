package at.hannibal2.skyhanni.utils.renderables.animated.rotate

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.renderables.animated.TimeDependentRenderable
import at.hannibal2.skyhanni.utils.renderables.decorators.RenderableDecorator
import net.minecraft.core.Direction.Axis
import kotlin.time.Duration

internal interface RotatingBehavior : AnimatedRotationStorage {
    val rotationStorage: AnimatedRotationStorage
    override val rotationDefinition: AnimatedRotationDefinition get() = rotationStorage.rotationDefinition
    override var currentRotation: SnappedVec3
        get() = rotationStorage.currentRotation
        set(value) {
            rotationStorage.currentRotation = value
        }

    fun applyRotation(deltaTime: Duration) {
        currentRotation = generateNextRotation(deltaTime.inPartialSeconds)
    }

    private fun generateNextRotation(deltaTime: Double): SnappedVec3 {
        if (!rotationDefinition.isEnabled()) return SnappedVec3.ZERO
        return Axis.entries.fold(currentRotation) { vec, axis ->
            if (!rotationDefinition.isAxisEnabled(axis)) vec
            else vec.applyAxisValue(axis, rotationDefinition.getRotation(axis, deltaTime))
        }
    }
}

class RotatingRenderable private constructor(
    override val root: Renderable,
    override val rotationStorage: AnimatedRotationStorage,
) : RenderableDecorator, TimeDependentRenderable, RotatingBehavior {
    override val height: Int get() = root.height
    override val width: Int get() = root.width
    override val horizontalAlign get() = root.horizontalAlign
    override val verticalAlign get() = root.verticalAlign
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        applyRotation(deltaTime)
    }
}
