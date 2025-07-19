package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.CircularRenderable.Companion.circular

@SkyHanniModule(devOnly = true)
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
