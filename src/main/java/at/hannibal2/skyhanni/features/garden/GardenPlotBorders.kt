package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.renderPlot
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds

object GardenPlotBorders {

    private val config get() = GardenAPI.config.plotBorders
    private var timeLastSaved = SimpleTimeMark.farPast()
    private var showBorders = false

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        if (timeLastSaved.passedSince() < 250.milliseconds) return

        if (event.keyCode == Keyboard.KEY_G && Keyboard.isKeyDown(Keyboard.KEY_F3)) {
            timeLastSaved = SimpleTimeMark.now()
            showBorders = !showBorders
        }
        if (event.keyCode == Keyboard.KEY_F3 && Keyboard.isKeyDown(Keyboard.KEY_G)) {
            timeLastSaved = SimpleTimeMark.now()
            showBorders = !showBorders
        }
    }

    @SubscribeEvent
    fun render(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!showBorders) return
        val plot = GardenPlotAPI.getCurrentPlot() ?: return
        event.renderPlot(plot, LorenzColor.YELLOW.toColor(), LorenzColor.DARK_BLUE.toColor())
    }


    fun isEnabled() = GardenAPI.inGarden() && config
}
