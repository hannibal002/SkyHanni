package at.hannibal2.skyhanni.config.features.inventory.customwardrobe

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.utils.InputCode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class KeybindConfig {
    @Expose
    @ConfigOption(
        name = "Slot Keybinds Toggle",
        desc = "Enable/Disable the slot keybinds.\n§cThis only works inside the Custom Wardrobe GUI."
    )
    @ConfigEditorBoolean
    var slotKeybindsToggle: Boolean = true

    @Expose
    @ConfigOption(name = "Slot 1", desc = "Keybind for slot 1")
    @ConfigEditorKeyMapping(defaultKey = KEY_1)
    var slot1 = InputCode.KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for slot 2")
    @ConfigEditorKeyMapping(defaultKey = KEY_2)
    var slot2 = InputCode.KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for slot 3")
    @ConfigEditorKeyMapping(defaultKey = KEY_3)
    var slot3 = InputCode.KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for slot 4")
    @ConfigEditorKeyMapping(defaultKey = KEY_4)
    var slot4 = InputCode.KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for slot 5")
    @ConfigEditorKeyMapping(defaultKey = KEY_5)
    var slot5 = InputCode.KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for slot 6")
    @ConfigEditorKeyMapping(defaultKey = KEY_6)
    var slot6 = InputCode.KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for slot 7")
    @ConfigEditorKeyMapping(defaultKey = KEY_7)
    var slot7 = InputCode.KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for slot 8")
    @ConfigEditorKeyMapping(defaultKey = KEY_8)
    var slot8 = InputCode.KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for slot 9")
    @ConfigEditorKeyMapping(defaultKey = KEY_9)
    var slot9 = InputCode.KEY_9
}
