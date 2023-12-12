package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.currentSpray
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUtils.color
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SprayDisplay {
    private val config get() = PestAPI.config.spray
    private var display: String? = null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled() || !event.isMod(4)) return
        val plot = GardenPlotAPI.getCurrentPlot() ?: return
        display = plot.currentSpray?.let {
            val timeUntilExpiry = it.expiry.timeUntil()
            "Sprayed with &a${it.type.displayName} &7- ${timeUntilExpiry.color()}${timeUntilExpiry.format()}"
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val display = display ?: return

        config.displayPosition.renderString(display, posLabel = "Active Plot Spray Display")
    }

    fun isEnabled() = GardenAPI.inGarden() && config.displayEnabled

}
