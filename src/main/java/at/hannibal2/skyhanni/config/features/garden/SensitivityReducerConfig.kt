package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

class SensitivityReducerConfig {
    @Expose
    @ConfigOption(name = "Mode", desc = "Lower mouse sensitivity while in the garden.")
    @ConfigEditorDropdown
    var mode: Mode = Mode.OFF

    enum class Mode(private val displayName: String) {
        OFF("Disabled"),
        TOOL("Holding farming tool"),
        KEYBIND("Holding Keybind"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Keybind", desc = "When selected above, press this key to reduce the mouse sensitivity.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_N)
    var keybind: Int = Keyboard.KEY_N

    @Expose
    @ConfigOption(name = "Reducing factor", desc = "Change by how much the sensitivity is lowered by.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 50f, minStep = 1f)
    val reducingFactor: Property<Float> = Property.of(15f)

    @Expose
    @ConfigOption(name = "Show GUI", desc = "Show the GUI element while the feature is enabled.")
    @ConfigEditorBoolean
    var showGui: Boolean = true

    @Expose
    @ConfigOption(name = "Only in Ground", desc = "Lower sensitivity when standing on the ground.")
    @ConfigEditorBoolean
    val onGround: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Disable in Barn", desc = "Disable reduced sensitivity in barn plot.")
    @ConfigEditorBoolean
    val onlyPlot: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = SensitivityReducerConfig::class, field = "showGui")
    val position: Position = Position(400, 400, 0.8f)
}
