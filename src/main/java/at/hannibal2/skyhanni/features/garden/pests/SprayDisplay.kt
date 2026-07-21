package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.garden.GardenPlotSprayEvent
import at.hannibal2.skyhanni.events.garden.PlotChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isSprayExpired
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.markExpiredSprayAsNotified
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.name
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.plots
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.TextUtils.createCommaSeparatedList
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColorChatFormatting
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@SkyHanniModule
object SprayDisplay {

    private val config get() = PestApi.config.spray
    private var display: Component = Component.empty()
    private var currentSprayPlot: GardenPlotApi.Plot? = null

    @HandleEvent
    fun onSecondPassed() {
        if (config.expiryNotification) {
            sendExpiredPlotsToChat(false)
        }
    }

    @HandleEvent
    fun onPlotChange(event: PlotChangeEvent) {
        val newPlot = event.plot
        if (newPlot == null) {
            currentSprayPlot = null
            return
        }
        if (config.displayEnabled) display = buildDisplay()
        currentSprayPlot = newPlot.takeUnless { it.isBarn() }
    }

    @HandleEvent
    fun onGardenPlotSprayChanged(event: GardenPlotSprayEvent) {
        if (config.displayEnabled) display = buildDisplay()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onIslandJoin(event: IslandJoinEvent) {
        if (!config.expiryNotification) return
        sendExpiredPlotsToChat(true)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.displayEnabled) return
        val display = Renderable.text(
            componentBuilder {
                append(display)
                append(buildTimerComponent())
            },
        )

        config.displayPosition.renderRenderable(display, posLabel = "Active Plot Spray Display")
    }

    private fun buildDisplay(): Component {
        val sprayData = currentSprayPlot?.currentSpray
            ?: return if (config.showNotSprayed) componentBuilder {
                appendWithColor("Not Sprayed!", ChatFormatting.RED)
            } else Component.empty()
        return componentBuilder {
            appendWithColor("Sprayed with ", ChatFormatting.YELLOW)
            appendWithColor(sprayData.type.displayName, ChatFormatting.GREEN)
            appendWithColor(" - ", ChatFormatting.GRAY)
        }
    }

    private fun buildTimerComponent(): Component {
        val timer = currentSprayPlot?.currentSpray?.expiry?.timeUntil() ?: return Component.empty()
        return componentBuilder {
            appendWithColor(timer.format(), timer.timerColorChatFormatting(ChatFormatting.AQUA))
        }
    }

    private fun sendExpiredPlotsToChat(wasAway: Boolean) {
        val expiredPlots = plots.filter { it.isSprayExpired }
        if (expiredPlots.isEmpty()) return

        expiredPlots.forEach { it.markExpiredSprayAsNotified() }
        val expiredSprayMessages = componentBuilder {
            val wasAwayString = if (wasAway) "While you were away, your" else "Your"
            appendWithColor(wasAwayString, ChatFormatting.GRAY)
            val sprayString = "spray".pluralize(expiredPlots.size)
            appendWithColor("$sprayString on ", ChatFormatting.GRAY)
            appendWithColor("Plot ", ChatFormatting.GREEN)
            appendWithColor("- ", ChatFormatting.GRAY)
            val plotsComponent = expiredPlots.map { it.name }
                .createCommaSeparatedList(ChatFormatting.AQUA, ChatFormatting.GRAY)
            append(plotsComponent)
            appendWithColor(" expired.", ChatFormatting.GRAY)
        }
        ChatUtils.chat(expiredSprayMessages)
    }
}
