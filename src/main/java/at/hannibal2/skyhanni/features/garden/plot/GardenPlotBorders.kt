package at.hannibal2.skyhanni.features.garden.plot

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi.renderPlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object GardenPlotBorders {
    private val config get() = GardenApi.config

    private var showBorders = false

    @HandleEvent
    fun onKeyDown(event: KeyDownEvent) {
        if (!isEnabled()) return
        if (MinecraftCompat.screen != null) return
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

    private fun getClosestPlot(): GardenPlot? =
        GardenPlotApi.plots.minByOrNull { it.middle.distanceSqToPlayer() }

    private fun isEnabled() = GardenApi.inGarden() && config.plotBorderKey != GLFW.GLFW_KEY_UNKNOWN
}
