package at.hannibal2.hanni.features.garden

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.KeyPressEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.garden.GardenPlotApi.renderPlot
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.SimpleTimeMark
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds

@HanniModule
object GardenPlotBorders {

    private val config get() = GardenApi.config.plotBorders
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var showBorders = false

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (!isEnabled()) return
        if (timeLastSaved.passedSince() < 250.milliseconds) return

        if (event.keyCode == Keyboard.KEY_G && Keyboard.KEY_F3.isKeyHeld()) {
            timeLastSaved = SimpleTimeMark.now()
            showBorders = !showBorders
        }
        if (event.keyCode == Keyboard.KEY_F3 && Keyboard.KEY_G.isKeyHeld()) {
            timeLastSaved = SimpleTimeMark.now()
            showBorders = !showBorders
        }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!showBorders) return
        val plot = GardenPlotApi.getCurrentPlot() ?: getClosestPlot() ?: return
        event.renderPlot(plot, LorenzColor.YELLOW.toColor(), LorenzColor.DARK_BLUE.toColor(), showBuildLimit = true)
    }

    private fun getClosestPlot(): GardenPlotApi.Plot? =
        GardenPlotApi.plots.minByOrNull { it.middle.distanceSqToPlayer() }

    fun isEnabled() = GardenApi.inGarden() && config
}
