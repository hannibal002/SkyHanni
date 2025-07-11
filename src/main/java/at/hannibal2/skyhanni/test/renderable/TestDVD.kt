package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.DVDLogoRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
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
