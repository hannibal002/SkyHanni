package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.DVDLogoRenderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString

@SkyHanniModule
object DVDTest {

    private val dvdRenderable by lazy {
        DVDLogoRenderable(
            renderable = RenderableString("§zDVD Logo Test", scale = 3.0),
            movementSpeed = 80f,
        )
    }

    private val position = Position(200, 200)

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!SkyHanniMod.feature.dev.debug.dvdLogo) return
        position.renderRenderable(dvdRenderable, posLabel = "DVDTest")
    }
}
