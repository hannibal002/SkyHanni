package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.garden.MouseSensitivityReducer.AutoEnableMode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class MouseSensitivityReducerConfig {
    @ConfigOption(
        name = "Note",
        desc = "You can type §e/shmouselock §rto lock your mouse rotation, and §e/shsensreduce §rto reduce your sensitivity.",
    )
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    @ConfigOption(name = "Auto Enable", desc = "Automatically lower mouse sensitivity while in the garden.")
    @ConfigEditorBoolean
    var autoEnable: Boolean = false

    @Expose
    @ConfigOption(name = "Auto Mode", desc = "Decide when the mouse sensitivity should be lowered.")
    @ConfigEditorDraggableList
    val autoEnableMode: MutableList<AutoEnableMode> = mutableListOf(AutoEnableMode.KEYBIND, AutoEnableMode.TOOL)

    @Expose
    @ConfigOption(name = "Keybind", desc = "When selected above, press this key to reduce the mouse sensitivity.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_N)
    var keybind: Int = GLFW.GLFW_KEY_N

    @Expose
    @ConfigOption(name = "Show GUI", desc = "Show the GUI element while the feature is enabled.")
    @ConfigEditorBoolean
    var showGui: Boolean = true

    @Expose
    @ConfigOption(name = "Chat Message", desc = "Show a message in chat when toggling §e/shmouselock §ror §e/shsensreduce§r.")
    @ConfigEditorBoolean
    var chatMessage: Boolean = true

    @Expose
    @ConfigOption(name = "Reducing Percent", desc = "Set sensitivity to this percentage of your normal sensitivity")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 99.9f, minStep = 0.1f)
    var reducingPercent: Float = 10f

    @Expose
    @ConfigOption(name = "Lock Mouse", desc = "Lock the mouse instead of reducing sensitivity.")
    @ConfigEditorBoolean
    var lockMouse: Boolean = false

    @Expose
    @ConfigOption(name = "Unlock on Teleport", desc = "Choose whether teleporting to a plot should unlock your mouse rotation.")
    @ConfigEditorDropdown
    var unlockOnTeleport: UnlockOnTeleport = UnlockOnTeleport.ALWAYS

    @Expose
    @ConfigOption(name = "Lock on Mousemat", desc = "Lock mouse when snapping to Squeaky Mousemat.")
    @ConfigEditorBoolean
    var lockOnMousemat: Boolean = true

    @Expose
    @ConfigOption(name = "Only on Ground", desc = "When enabled, lower sensitivity only while on or near the ground.")
    @ConfigEditorBoolean
    var onGround: Boolean = true

    @Expose
    @ConfigOption(
        name = "Only on Ground Tolerance",
        desc = "How close to ground counts as on ground when 'Only on Ground' is enabled. Useful for farms with small height drops.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 2f, minStep = 1f / 16f)
    var onGroundTolerance: Float = 2f / 16f // dirt to soulsand is 2 pixels

    @Expose
    @ConfigOption(name = "Disable in Barn or Greenhouse", desc = "Disable reduced sensitivity in barn and greenhouse plots.")
    @ConfigEditorBoolean
    var onlyPlot: Boolean = true

    @Expose
    @ConfigLink(owner = MouseSensitivityReducerConfig::class, field = "showGui")
    val position: Position = Position(400, 200)

    enum class UnlockOnTeleport(private val displayName: String, val condition: (String) -> Boolean) {
        ALWAYS("Always", { it != "Warping..." }), // TODO: decide what to do with /warp garden
        BARN_ONLY("Barn Only", { it == "The Barn" }),
        NEVER("Never", { false }),
        ;

        override fun toString() = displayName
    }
}
