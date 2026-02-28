package at.hannibal2.skyhanni.utils.renderables.animated.rotate

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.SnappedVec3
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemRenderableConfig
import at.hannibal2.skyhanni.utils.renderables.animated.TimeDependentRenderable
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.decorators.RenderableDecoratorOnlyRender
import net.minecraft.core.Direction.Axis
import kotlin.time.Duration

internal interface RotatingBehavior : AnimatedRotationStorage {
    val config: AnimatedItemRenderableConfig<*>
    val rotationStorage: AnimatedRotationStorage get() = config.rotationStorage
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
        return Axis.entries.filter { rotationDefinition.isAxisEnabled(it) }.fold(currentRotation) { vec, axis ->
            val offset = rotationDefinition.getRotationOffset(axis, deltaTime)
            vec.applyAxisOffset(axis, offset)
        }
    }
}

class RotatingRenderable private constructor(
    override val root: Renderable,
    override val config: AnimatedItemRenderableConfig<*>,
) : RenderableDecoratorOnlyRender, TimeDependentRenderable, RotatingBehavior {
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()
    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        applyRotation(deltaTime)
    }

    companion object {
        fun <C : AnimatedFrame> Renderable.Companion.rotating(
            root: Renderable,
            config: AnimatedItemRenderableConfig<C>.() -> Unit = { }
        ) = RotatingRenderable(root, AnimatedItemRenderableConfig<C>().apply(config))
    }
}
