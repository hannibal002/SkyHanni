package at.hannibal2.skyhanni.config.features.mining.nucleus

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class CrystalNucleusSpeedrunConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable the Nucleus Speedrun timer.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Quick Disable", desc = "You can use §e/shnucspeedrun §rto quickly toggle all functions of this module.")
    @ConfigEditorText
    var ignore: Nothing? = null

    @Expose
    @ConfigOption(name = "Split Type", desc = "Whether split times should be set automatically on collecting a crystal, or manually.")
    @ConfigEditorDropdown
    var splitType: SplitType = SplitType.AUTO

    enum class SplitType(private val displayName: String) {
        AUTO("Auto"),
        MANUAL("Manual"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Run Start Type", desc = "What should start the initial timer for the run.")
    @ConfigEditorDropdown
    var runStartType: RunStartType = RunStartType.LEAVE_CRYSTAL_NUCLEUS

    enum class RunStartType(private val displayName: String) {
        LEAVE_CRYSTAL_NUCLEUS("Leave Crystal Nucleus"),
        HOTKEY("Hotkey"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Hotkey Assignment", desc = "The hotkey to both start the timer, and perform a split.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var hotkey: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Wait for Warp + Move",
        desc = "If enabled, after a split is performed, the timer will wait for the player to §e/warp nucleus §rand move before starting" +
            "the next split's timer.\n" +
            "§cShould only be used for those with §bMVP§f+ §cwho can unlock the travel scroll§r."
    )
    @ConfigEditorBoolean
    var waitForWarpAndMove: Boolean = true

    @Expose
    @ConfigOption(
        name = "Rolling Average Count",
        desc = "How many split times to keep in storage to calculate the rolling average.\n" +
            "§eLarger numbers will be more accurate, but will use more storage space, and may cause memory allocation issues§r."
    )
    @ConfigEditorSlider(minValue = 10f, maxValue = 1000f, minStep = 1f)
    var rollingAverageCount: Int = 25
}
