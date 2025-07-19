package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.RadialGradientCircularRenderable.Companion.radialGradientCircular

@SkyHanniModule(devOnly = true)
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
