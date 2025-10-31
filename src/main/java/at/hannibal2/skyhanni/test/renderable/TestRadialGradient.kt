package at.hannibal2.hanni.test.renderable

import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.animated.RadialGradientCircularRenderable.Companion.radialGradientCircular

@HanniModule(devOnly = true)
object TestRadialGradient : RenderableTestSuite.TestRenderable("radial_gradient") {
    private val gradientCircularRenderable by lazy {
        with(Renderable) {
            radialGradientCircular(
                startColor = LorenzColor.BLUE.toChromaColor(),
                endColor = LorenzColor.AQUA.toChromaColor(),
                radius = 20,
            )
        }
    }

    override fun renderable() = gradientCircularRenderable
}
