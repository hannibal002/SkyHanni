package at.hannibal2.hanni.test.renderable

import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.CircularRenderable.Companion.circular

@HanniModule(devOnly = true)
object TestCircular : RenderableTestSuite.TestRenderable("circle") {
    override fun renderable() = with(Renderable) {
        circular(
            backgroundColor = LorenzColor.LIGHT_PURPLE.toChromaColor(),
            filledPercentage = 75.0,
            unfilledColor = LorenzColor.GRAY.toChromaColor(),
            radius = 30,
        )
    }
}
