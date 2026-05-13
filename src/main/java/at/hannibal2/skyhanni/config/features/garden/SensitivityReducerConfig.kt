package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class SensitivityReducerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Lower mouse sensitivity while in the garden.")
    @ConfigEditorBoolean
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Mode", desc = "Decide when the mouse sensitivity should be lowered.")
    @ConfigEditorDraggableList
    val mode: MutableList<Mode> = mutableListOf(Mode.TOOL)

    enum class Mode(private val displayName: String) {
        TOOL("Farming tool"),
        FISHING_ROD("Fishing Rod"),
        KEYBIND("Holding Keybind"),
        MOUSEMAT("Squeaky Mousemat"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Keybind", desc = "When selected above, press this key to reduce the mouse sensitivity.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_N)
    var keybind: Int = GLFW.GLFW_KEY_N

    @Expose
    @ConfigOption(name = "Reducing factor", desc = "Change by how much the sensitivity is lowered by.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 50f, minStep = 1f)
    val reducingFactor: Property<Float> = Property.of(15f)

    @Expose
    @ConfigOption(name = "Show GUI", desc = "Show the GUI element while the feature is enabled.")
    @ConfigEditorBoolean
    var showGui: Boolean = true

    @Expose
    @ConfigOption(name = "Only on Ground", desc = "When enabled, lower sensitivity only while on or near the ground.")
    @ConfigEditorBoolean
    val onGround: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Only on Ground Tolerance",
        desc = "How close to ground counts as on ground when 'Only on Ground' is enabled. Useful for farms with small height drops.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 2f, minStep = 1f / 16f) // Block heights are multiples of 1/16
    val onGroundTolerance: Property<Float> = Property.of(0f)

    @Expose
    @ConfigOption(name = "Disable in Barn", desc = "Disable reduced sensitivity in barn plot.")
    @ConfigEditorBoolean
    val onlyPlot: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = SensitivityReducerConfig::class, field = "showGui")
    val position: Position = Position(400, 400, 0.8f)
}
