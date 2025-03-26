package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.createResourceLocation

@SkyHanniModule
object SkyHanniLogoDVD {

    private val config get() = SkyHanniMod.feature.misc
    private val enabled get() = config.dvdLogoEnabled

    private val skyHanniLogoResourceLocation = createResourceLocation("")

    private val skyHanniLogoRenderable by lazy {
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {

    }
}
