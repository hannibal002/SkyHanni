package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.renderables.CircularRenderable
import at.hannibal2.skyhanni.utils.renderables.OrbitSystemRenderable
import java.awt.Color

@SkyHanniModule(devOnly = true)
object TestOrbital : RenderableTestSuite.TestRenderable("orbital") {

    private val orbitalRenderable by lazy {
        OrbitSystemRenderable(
            mainBody = CircularRenderable(
                backgroundColor = Color.BLUE.toChromaColor(255),
                radius = 10,
            ),
            subBodies = listOf(
                CircularRenderable(backgroundColor = Color.RED.toChromaColor(255), radius = 5),
                CircularRenderable(backgroundColor = Color.GREEN.toChromaColor(255), radius = 5),
                CircularRenderable(backgroundColor = Color.YELLOW.toChromaColor(255), radius = 5),
                CircularRenderable(backgroundColor = Color.ORANGE.toChromaColor(255), radius = 5),
            ),
            orbitSpeed = 40,
        )
    }

    override fun renderable() = orbitalRenderable
}
