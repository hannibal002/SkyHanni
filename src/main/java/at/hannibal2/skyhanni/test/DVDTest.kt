package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.DVDLogoRenderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable

@SkyHanniModule
object DVDTest {

    private val config get() = SkyHanniMod.feature.dev.debug
    private val dvdRenderable by lazy {
        DVDLogoRenderable(
            renderable = StringRenderable("§zDVD Logo Test", scale = 3.0),
            movementSpeed = 80f,
        )
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.dvdLogo) return
        config.dvdLogoPosition.renderRenderable(dvdRenderable, posLabel = "DVDTest")
    }
}
