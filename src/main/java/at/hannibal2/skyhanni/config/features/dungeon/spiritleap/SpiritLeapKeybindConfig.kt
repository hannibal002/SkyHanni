package at.hannibal2.skyhanni.config.features.dungeon.spiritleap

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class SpiritLeapKeybindConfig {
    @Expose
    @ConfigOption(
        name = "Leap Keybinds",
        desc = "Enable or disable the Spirit Leap keybinds.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enableKeybind = false

    @Expose
    @ConfigOption(
        name = "Display Keybind Hints",
        desc = "Show keybind hints to indicate which key to press for leap menu."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var showKeybindHint = false

    @Expose
    @ConfigOption(
        name = "Keybind: First Target",
        desc = "Keybind for teleporting to the first available Spirit Leap target."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_1)
    var keybindOption1 = Keyboard.KEY_1

    @Expose
    @ConfigOption(
        name = "Keybind: Second Target",
        desc = "Keybind for teleporting to the second available Spirit Leap target."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_2)
    var keybindOption2 = Keyboard.KEY_2

    @Expose
    @ConfigOption(
        name = "Keybind: Third Target",
        desc = "Keybind for teleporting to the third available Spirit Leap target."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_3)
    var keybindOption3 = Keyboard.KEY_3

    @Expose
    @ConfigOption(
        name = "Keybind: Fourth Target",
        desc = "Keybind for teleporting to the fourth available Spirit Leap target."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_4)
    var keybindOption4 = Keyboard.KEY_4
}
