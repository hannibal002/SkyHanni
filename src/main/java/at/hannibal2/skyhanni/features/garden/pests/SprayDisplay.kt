package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.garden.PlotChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi.plots
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColorChatFormatting
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.createCommaSeparatedList
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@SkyHanniModule
object SprayDisplay {

    private val config get() = PestApi.config.spray
    private var staticDisplayLines: Component = Component.empty()
    private var currentSprayPlot: GardenPlot? = null
    private var builtDisplay: Renderable? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onSecondPassed() {
        if (config.expiryNotification) {
            sendExpiredPlotsToChat(false)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (staticDisplayLines == Component.empty()) return
        builtDisplay = Renderable.text(
            componentBuilder {
                append(staticDisplayLines)
                append(buildTimerComponent())
            },
        )
    }

    @HandleEvent
    private fun onPlotChange(event: PlotChangeEvent) {
        val newPlot = event.plot
        if (newPlot == null) {
            currentSprayPlot = null
            staticDisplayLines = Component.empty()
            return
        }
        currentSprayPlot = newPlot
        if (config.displayEnabled) staticDisplayLines = buildDisplay()
    }

    @HandleEvent
    private fun onGardenPlotSprayChanged() {
        if (config.displayEnabled) staticDisplayLines = buildDisplay()
    }

    @HandleEvent
    private fun onIslandLeave(event: IslandLeaveEvent) {
        if (event.island == IslandType.GARDEN) {
            builtDisplay = null
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onIslandJoin() {
        if (!config.expiryNotification) return
        sendExpiredPlotsToChat(true)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onGuiRenderOverlay() {
        if (!config.displayEnabled) return
        val display = builtDisplay ?: return

        config.displayPosition.renderRenderable(display, posLabel = "Active Plot Spray Display")
    }

    private fun buildDisplay(): Component {
        val sprayData = currentSprayPlot?.currentSpray
            ?: return if (config.showNotSprayed) componentBuilder {
                appendWithColor("Not sprayed!", ChatFormatting.RED)
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
            appendWithColor(" $sprayString on ", ChatFormatting.GRAY)
            appendWithColor("Plot", ChatFormatting.GREEN)
            appendWithColor(" - ", ChatFormatting.GRAY)
            val plotsComponent = expiredPlots.map { it.name.asComponent().withColor(ChatFormatting.AQUA) }
                .createCommaSeparatedList(ChatFormatting.GRAY)

            append(plotsComponent)
            appendWithColor(" expired.", ChatFormatting.GRAY)
        }
        ChatUtils.chat(expiredSprayMessages)
    }
}
