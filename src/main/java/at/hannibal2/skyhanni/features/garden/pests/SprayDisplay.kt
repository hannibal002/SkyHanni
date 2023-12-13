package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isSprayExpired
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.markExpiredSprayAsNotified
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.name
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.plots
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUtils.timerColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SprayDisplay {
    private val config get() = PestAPI.config.spray
    private var display: String? = null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return

        if (isDisplayEnabled()) {
            val plot = GardenPlotAPI.getCurrentPlot() ?: return
            display = plot.currentSpray?.let {
                val timer = it.expiry.timeUntil()
                "§eSprayed with §a${it.type.displayName} §7- ${timer.timerColor("§b")}${timer.format()}"
            }
        }

        if (isNotificationEnabled()) {
            sendExpiredPlotsToChat(false)
        }
    }

    @SubscribeEvent
    fun onJoin(event: IslandChangeEvent) {
        if (!isNotificationEnabled() || event.newIsland != IslandType.GARDEN) return
        if (event.newIsland == event.oldIsland) return

        sendExpiredPlotsToChat(true)
    }

    @SubscribeEvent
    fun onRenderOverlay(ignored: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isDisplayEnabled()) return

        val display = display ?: return

        config.displayPosition.renderString(display, posLabel = "Active Plot Spray Display")
    }

    private fun isDisplayEnabled() = GardenAPI.inGarden() && config.displayEnabled
    private fun isNotificationEnabled() = GardenAPI.inGarden() && config.expiryNotification

    private fun sendExpiredPlotsToChat(wasAway: Boolean) {
        val expiredPlots = plots.filter { it.isSprayExpired }
        if (expiredPlots.isEmpty()) return

        expiredPlots.forEach { it.markExpiredSprayAsNotified() }
        val wasAwayString = if (wasAway) "While you were away, §7your" else "§7Your"
        val plotString = StringUtils.createCommaSeparatedList(expiredPlots.map { "§b${it.name}" }, "§7, ")
        val sprayString = if (expiredPlots.size > 1) "sprays" else "spray"
        val out = "§e[SkyHanni] ${wasAwayString} $sprayString on §aPlot §7- $plotString §7expired."
        LorenzUtils.chat(out)
    }

}
