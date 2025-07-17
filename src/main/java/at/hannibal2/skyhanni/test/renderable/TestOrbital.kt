package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.OrbitSystemRenderable.Companion.orbitalSystem
import at.hannibal2.skyhanni.utils.renderables.primitives.CircularRenderable.Companion.circular
import java.awt.Color

@SkyHanniModule(devOnly = true)
object TestOrbital : RenderableTestSuite.TestRenderable("orbital") {

    private val orbitalRenderable by lazy {
        with(Renderable) {
            orbitalSystem(
                mainBody = circular(
                    backgroundColor = Color.BLUE.toChromaColor(255),
                    radius = 10,
                ),
                subBodies = listOf(
                    circular(backgroundColor = Color.RED.toChromaColor(255), radius = 5),
                    circular(backgroundColor = Color.GREEN.toChromaColor(255), radius = 5),
                    circular(backgroundColor = Color.YELLOW.toChromaColor(255), radius = 5),
                    circular(backgroundColor = Color.ORANGE.toChromaColor(255), radius = 5),
                ),
                orbitSpeed = 40,
            )
        }
    }

    override fun renderable() = orbitalRenderable
}
