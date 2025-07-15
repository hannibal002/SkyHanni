package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class TrackCommandConfig {
    @Expose
    @NoConfigLink
    val position: Position = Position(0, 0)

    @Expose
    @ConfigOption(name = "Recency Window", desc = "How recent an event must (<= x seconds) be to be displayed.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var recencyWindow: Int = 3

    @Expose
    @ConfigOption(name = "Max List Length", desc = "Maximum number of events to display in the list.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 100f, minStep = 1f)
    var maxListLength: Int = 10

    @Expose
    @ConfigOption(name = "Keybind", desc = "Press this keybind to start/stop tracking.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var toggleKeybind: Int = Keyboard.KEY_NONE
}
