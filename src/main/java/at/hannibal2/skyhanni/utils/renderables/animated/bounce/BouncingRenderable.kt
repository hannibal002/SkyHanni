package at.hannibal2.skyhanni.utils.renderables.animated.bounce

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.renderables.animated.TimeDependentRenderable
import at.hannibal2.skyhanni.utils.renderables.decorators.RenderableDecorator
import net.minecraft.core.Direction.Axis
import kotlin.math.sin
import kotlin.time.Duration

internal interface BouncingBehavior : AnimatedBounceStorage {
    val bounceStorage: AnimatedBounceStorage
    val bounceStartTime: SimpleTimeMark

    override val bounceDefinition: AnimatedBounceDefinition get() = bounceStorage.bounceDefinition
    override var currentBounce: SnappedVec3
        get() = bounceStorage.currentBounce
        set(value) {
            bounceStorage.currentBounce = value
        }

    val bounceExtraHeight: Int get() = bounceDefinition.getTotalBounceOffset(Axis.Y)
    val bounceExtraWidth: Int get() = bounceDefinition.getTotalBounceOffset(Axis.X)

    fun applyBounce() {
        currentBounce = generateNextBounce()
    }

    private fun generateNextBounce(): SnappedVec3 {
        if (!bounceDefinition.isEnabled()) return SnappedVec3.ZERO
        val t = bounceStartTime.passedSince().inPartialSeconds
        return Axis.entries.fold(currentBounce) { vec, axis ->
            if (!bounceDefinition.isAxisEnabled(axis)) return@fold vec
            val bounceOffset = bounceDefinition.getTotalBounceOffset(axis)
            val axisPeriod = bounceDefinition.getBouncePeriod(axis)
            val theta = (t % axisPeriod) / axisPeriod * (2 * Math.PI)
            val bounceCoordinateOffset = sin(theta) * bounceDefinition.getBounceOffset(axis, sin(theta))
            val bounceCoordinate = (bounceOffset / 2.0) + bounceCoordinateOffset
            vec.applyAxisValue(axis, bounceCoordinate)
        }
    }
}

class BouncingRenderable(
    override val root: Renderable,
    override val bounceStorage: AnimatedBounceStorage,
) : RenderableDecorator, TimeDependentRenderable, BouncingBehavior {
    override val bounceStartTime: SimpleTimeMark = SimpleTimeMark.now()
    override val height: Int get() = root.height + bounceExtraHeight
    override val width: Int get() = root.width + bounceExtraWidth
    override val horizontalAlign get() = root.horizontalAlign
    override val verticalAlign get() = root.verticalAlign
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        applyBounce()
    }
}
