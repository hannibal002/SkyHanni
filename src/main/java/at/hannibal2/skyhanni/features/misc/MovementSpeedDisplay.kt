package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

class MovementSpeedDisplay {
    private val config get() = SkyHanniMod.feature.misc
    private var lastLocation: LorenzVec? = null
    private var display = ""

    init {
        fixedRateTimer(name = "skyhanni-movement-speed-display", period = 250, initialDelay = 1_000) {
            checkSpeed()
        }
    }

    private fun checkSpeed() {
        if (!isEnabled()) return

        val currentLocation = LocationUtils.playerLocation()
        if (lastLocation == null) {
            lastLocation = currentLocation
            return
        }

        lastLocation?.let {
            val distance = it.distance(currentLocation) * 4
            display = "Movement Speed: ${distance.round(2)}"
            lastLocation = currentLocation
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.playerMovementSpeedPos.renderString(display, posLabel = "Movement Speed")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.playerMovementSpeed
}
