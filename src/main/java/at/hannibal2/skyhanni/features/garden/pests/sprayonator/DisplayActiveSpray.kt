package at.hannibal2.skyhanni.features.garden.pests.sprayonator

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.garden.GardenPlotSprayDataTablistReadEvent
import at.hannibal2.skyhanni.events.garden.PlotChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
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
object DisplayActiveSpray {

    private val config get() = PestApi.config.spray.SprayDisplay
    private var staticDisplayLines: Component? = null
    private var currentSprayPlot: GardenPlot? = null
    private var builtDisplay: Renderable? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (!config.displayEnabled) return

        val currentStaticLines = staticDisplayLines
        if (currentStaticLines == null) {
            builtDisplay = null
            return
        }
        builtDisplay = Renderable.text(
            componentBuilder {
                append(currentStaticLines)
                append(buildTimerComponent())
            },
        )
    }

    @HandleEvent
    private fun onPlotChange(event: PlotChangeEvent) {
        val newPlot = event.plot
        if (newPlot == null) {
            currentSprayPlot = null
            staticDisplayLines = null
            return
        }
        currentSprayPlot = newPlot.takeUnless { it.isBarn() || (it.greenhouse && config.hideInGreenhouse) }
        if (isDisplayEnabled()) staticDisplayLines = buildStaticDisplayLines()
    }

    @HandleEvent
    private fun onGardenPlotSprayChanged() {
        if (isDisplayEnabled()) staticDisplayLines = buildStaticDisplayLines()
    }

    @HandleEvent
    private fun onIslandLeave(event: IslandLeaveEvent) {
        if (event.island == IslandType.GARDEN) {
            builtDisplay = null
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onGuiRenderOverlay() {
        if (!isDisplayEnabled()) return
        val display = builtDisplay ?: return

        config.displayPosition.renderRenderable(display, posLabel = "Active Plot Spray Display")
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onGardenSprayTablistRead(event: GardenPlotSprayDataTablistReadEvent) {
        if (event.plotName != currentSprayPlot?.name) return
        if (isDisplayEnabled()) staticDisplayLines = buildStaticDisplayLines()
    }

    private fun buildStaticDisplayLines(): Component {
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


    private fun isDisplayEnabled() = config.displayEnabled
}
