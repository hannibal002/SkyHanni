package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.DVDLogoRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable

@SkyHanniModule
object DVDTest {

    private val dvdRenderable by lazy {
        DVDLogoRenderable(
            renderable = Renderable.string("§zTest string", scale = 3.0),
        )
    }

    private val position = Position(0, 0)

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!SkyHanniMod.feature.dev.debug.dvdLogo) return
        position.renderRenderable(dvdRenderable, posLabel = "DVDTest")
    }
}
