package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.SensitivityReducer.Mode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class SensitivityReducerConfig {
    @ConfigOption(
        name = "Note",
        desc = "You can type §e/shmouselock §rto lock your mouse rotation, and §e/shsensreduce §rto reduce your sensitivity.",
    )
    @ConfigEditorInfoText
    val notice: String = ""

    @Expose
    @ConfigOption(name = "Auto Enable", desc = "Automatically lower mouse sensitivity while in the garden.")
    @ConfigEditorBoolean
    val enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Mode", desc = "Decide when the mouse sensitivity should be lowered.")
    @ConfigEditorDraggableList
    val mode: MutableList<Mode> = mutableListOf(Mode.KEYBIND, Mode.TOOL)

    @Expose
    @ConfigOption(name = "Keybind", desc = "When selected above, press this key to reduce the mouse sensitivity.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_N)
    var keybind: Int = GLFW.GLFW_KEY_N

    @Expose
    @ConfigOption(name = "Reducing percent", desc = "Change by how much the sensitivity is lowered by.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 99.9f, minStep = 0.1f)
    val reducingPercent: Float = 10f

    @Expose
    @ConfigOption(name = "Lock mouse", desc = "Lock the mouse instead of reducing sensitivity.")
    @ConfigEditorBoolean
    val lockMouse: Boolean = false

    @Expose
    @ConfigOption(name = "Show GUI", desc = "Show the GUI element while the feature is enabled.")
    @ConfigEditorBoolean
    var showGui: Boolean = true

    @Expose
    @ConfigOption(name = "Only on Ground", desc = "When enabled, lower sensitivity only while on or near the ground.")
    @ConfigEditorBoolean
    val onGround: Boolean = true

    @Expose
    @ConfigOption(
        name = "Only on Ground Tolerance",
        desc = "How close to ground counts as on ground when 'Only on Ground' is enabled. Useful for farms with small height drops.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 2f, minStep = 1f / 16f) // Block heights are multiples of 1/16
    val onGroundTolerance: Float = 2f / 16f // dirt to soulsand is 2 pixels

    @Expose
    @ConfigOption(name = "Disable in Barn or Greenhouse", desc = "Disable reduced sensitivity in barn and greenhouse plots.")
    @ConfigEditorBoolean
    val onlyPlot: Boolean = true

    @Expose
    @ConfigOption(name = "Unlock on Teleport", desc = "Choose whether teleporting to a plot should unlock your mouse rotation.")
    @ConfigEditorBoolean
    val unlockOnTeleport: UnlockOnTeleport = UnlockOnTeleport.ALWAYS

    @Expose
    @ConfigLink(owner = SensitivityReducerConfig::class, field = "showGui")
    val display: Position = Position(400, 200)

    enum class UnlockOnTeleport(private val displayName: String, val condition: (String) -> Boolean) {
        ALWAYS("Always", { true }),
        BARN_ONLY("Barn Only", { it == "The Barn" }),
        NEVER("Never", { false }),
        ;

        override fun toString() = displayName
    }
}
