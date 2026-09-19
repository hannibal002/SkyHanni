package at.hannibal2.skyhanni.utils.renderables.animated.bounce

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemRenderableConfig
import net.minecraft.core.Direction.Axis
import kotlin.math.sin

internal interface BouncingBehavior : AnimatedBounceStorage {
    val config: AnimatedItemRenderableConfig<*>
    val bounceStorage: AnimatedBounceStorage get() = config.bounceStorage
    val bounceStartTime: SimpleTimeMark

    override val bounceDefinition: AnimatedBounceDefinition get() = bounceStorage.bounceDefinition
    override var currentBounce: SnappedVec3
        get() = bounceStorage.currentBounce
        set(value) {
            bounceStorage.currentBounce = value
        }

    fun applyBounce() {
        currentBounce = generateBounce()
    }

    private fun generateBounce(): SnappedVec3 {
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
