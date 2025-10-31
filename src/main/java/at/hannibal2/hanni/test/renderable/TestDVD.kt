package at.hannibal2.hanni.test.renderable

import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.animated.DVDLogoRenderable.Companion.dvdLogo
import at.hannibal2.hanni.utils.renderables.primitives.text

@HanniModule(devOnly = true)
object TestDVD : RenderableTestSuite.TestRenderable("dvd") {

    private val dvdRenderable by lazy {
        Renderable.dvdLogo(
            renderable = Renderable.text("§zDVD Logo Test", scale = 3.0),
            movementSpeed = 80f,
        )
    }

    override fun renderable() = dvdRenderable
}
