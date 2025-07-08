package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class TrackCommandConfig {
    // No config element, so no ConfigLink intentionally
    @Expose
    val position: Position = Position(0, 0)

    @Expose
    @ConfigOption(name = "Keybind", desc = "Press this keybind to start/stop tracking.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var toggleKeybind: Int = Keyboard.KEY_NONE
}
