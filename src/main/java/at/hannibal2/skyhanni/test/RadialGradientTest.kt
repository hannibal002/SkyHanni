package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.RadialGradientCircularRenderable

@SkyHanniModule
object RadialGradientTest {

    private val config get() = SkyHanniMod.feature.dev.debug
    private val gradientCircularRenderable by lazy {
        RadialGradientCircularRenderable(
            startColor = LorenzColor.BLUE.toChromaColor(),
            endColor = LorenzColor.AQUA.toChromaColor(),
            radius = 20,
        )
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.radialGradient) return
        config.radialGradientPos.renderRenderable(gradientCircularRenderable, posLabel = "DVDTest")
    }
}
