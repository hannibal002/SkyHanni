package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.greenhouse
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isSprayExpired
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.markExpiredSprayAsNotified
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.name
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.plots
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.createCommaSeparatedList
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColor
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@SkyHanniModule
object SprayDisplay {

    private val config get() = PestApi.config.spray
    private var display: Renderable? = null
    private val currentSprayPlot get() = GardenPlotApi.currentPlot?.takeUnless { it.isBarn() || it.greenhouse }

    @HandleEvent
    fun onSecondPassed() {
        val currentPlot = currentSprayPlot ?: return
        if (config.displayEnabled) display = Renderable.text(buildDisplay(currentPlot))

        if (config.expiryNotification) {
            sendExpiredPlotsToChat(false)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onIslandChange(event: IslandChangeEvent) {
        if (!config.expiryNotification || event.newIsland != IslandType.GARDEN) return
        sendExpiredPlotsToChat(true)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.displayEnabled) return
        val display = display ?: return
        config.displayPosition.renderRenderable(display, posLabel = "Active Plot Spray Display")
    }

    private fun buildDisplay(plot: GardenPlotApi.Plot): Component {
        val sprayData = plot.currentSpray
            ?: return if (config.showNotSprayed) componentBuilder {
                appendWithColor("Not Sprayed!", ChatFormatting.RED)
            } else Component.empty()
        val timer = sprayData.expiry.timeUntil()
        return componentBuilder {
            appendWithColor("Sprayed with ", ChatFormatting.YELLOW)
            appendWithColor(sprayData.type.displayName, ChatFormatting.GREEN)
            appendWithColor(" - ", ChatFormatting.GRAY)
            appendWithColor(timer.format(), timer.timerColor(ChatFormatting.AQUA))
        }
    }

    private fun sendExpiredPlotsToChat(wasAway: Boolean) {
        val expiredPlots = plots.filter { it.isSprayExpired }
        if (expiredPlots.isEmpty()) return

        expiredPlots.forEach { it.markExpiredSprayAsNotified() }
        val wasAwayString = if (wasAway) "§7While you were away, your" else "§7Your"
        val plotString = expiredPlots.map { "§b${it.name}" }.createCommaSeparatedList("§7")
        val sprayString = "spray".pluralize(expiredPlots.size)
        val out = "$wasAwayString $sprayString on §aPlot §7- $plotString §7expired."
        ChatUtils.chat(out)
    }
}
