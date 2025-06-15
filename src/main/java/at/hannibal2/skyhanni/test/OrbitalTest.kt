package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.CircularRenderable
import at.hannibal2.skyhanni.utils.renderables.OrbitSystemRenderable
import java.awt.Color

@SkyHanniModule
object OrbitalTest {

    private val config get() = SkyHanniMod.feature.dev.debug
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

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.orbital) return
        config.orbitalPosition.renderRenderable(orbitalRenderable, posLabel = "Orbital Test")
    }
}
