package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.renderPlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object GardenPlotBorders {
    private val config get() = GardenApi.config

    private var showBorders = false

    @HandleEvent
    fun onKeyDown(event: KeyDownEvent) {
        if (!isEnabled()) return
        if (event.keyCode == config.plotBorderKey) {
            showBorders = !showBorders
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || !showBorders) return
        val plot = GardenPlotApi.getCurrentPlot() ?: getClosestPlot() ?: return
        event.renderPlot(
            plot,
            LorenzColor.YELLOW.toColor(),
            LorenzColor.DARK_BLUE.toColor(),
            showBuildLimit = true,
        )
    }

    private fun getClosestPlot(): GardenPlotApi.Plot? =
        GardenPlotApi.plots.minByOrNull { it.middle.distanceSqToPlayer() }

    private fun isEnabled() = GardenApi.inGarden() && config.plotBorderKey != GLFW.GLFW_KEY_UNKNOWN
}
