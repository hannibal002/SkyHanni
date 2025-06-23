package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration

/**
 * An abstract class for renderables that are dependent on time since last render to determine their state.
 */
abstract class TimeDependentRenderable : Renderable {
    private var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    abstract fun renderWithDelta(posX: Int, posY: Int, deltaTime: Duration)

    override fun render(posX: Int, posY: Int) {
        val now = SimpleTimeMark.now()
        val deltaTime = now - lastRenderTime
        lastRenderTime = now

        renderWithDelta(posX, posY, deltaTime)
    }
}
