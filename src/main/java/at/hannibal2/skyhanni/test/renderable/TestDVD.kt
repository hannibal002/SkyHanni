package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.renderables.DVDLogoRenderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable

@SkyHanniModule(devOnly = true)
object TestDVD : RenderableTestSuite.TestRenderable("dvd") {

    private val dvdRenderable by lazy {
        DVDLogoRenderable(
            renderable = StringRenderable("§zDVD Logo Test", scale = 3.0),
            movementSpeed = 80f,
        )
    }

    override fun renderable() = dvdRenderable
}
