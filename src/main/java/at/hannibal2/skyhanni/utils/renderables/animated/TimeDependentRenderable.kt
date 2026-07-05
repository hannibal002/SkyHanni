package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import kotlin.time.Duration

/**
 * An abstract class for renderables that are dependent on time since last render to determine their state.
 */
interface TimeDependentRenderable : Renderable {
    /**
     * Should be initialized to SimpleTimeMark.now()
     * Do not do anything else with this, as this provides the [deltaTime] for [renderWithDelta]
     */
    var lastRenderTime: SimpleTimeMark

    fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration)

    @Deprecated("Use renderWithDelta instead", ReplaceWith("renderWithDelta(posX, posY, deltaTime)"))
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        val now = SimpleTimeMark.now()
        val deltaTime = now - lastRenderTime
        lastRenderTime = now

        renderWithDelta(mouseOffsetX, mouseOffsetY, deltaTime)
    }
}
