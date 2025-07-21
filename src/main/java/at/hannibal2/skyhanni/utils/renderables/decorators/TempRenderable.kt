package at.hannibal2.skyhanni.utils.renderables.decorators

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable

/** Does hide itself if [deathTime] is reached, does still exist but does not show anything anymore*/
class TempRenderable private constructor(
    override val root: Renderable,
    private val deathTime: SimpleTimeMark,
) : RenderableDecoratorOnlyRender {

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        if (deathTime.isInPast()) return
        root.render(mouseOffsetX, mouseOffsetY)
    }

    companion object {
        fun Renderable.toTemp(deathTime: SimpleTimeMark) = TempRenderable(this, deathTime)
    }
}
